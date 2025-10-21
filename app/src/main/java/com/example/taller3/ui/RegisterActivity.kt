package com.example.taller3.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.load
import com.example.taller3.data.UserProfile
import com.example.taller3.data.UserRepository
import com.example.taller3.databinding.ActivityRegisterBinding
import com.example.taller3.util.Toaster
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class RegisterActivity : AppCompatActivity() {

    private lateinit var b: ActivityRegisterBinding
    private val repo = UserRepository()

    private var pickedImage: Uri? = null
    private var cameraImageUri: Uri? = null

    // ===== Launchers =====

    // Permiso de cámara
    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                launchCamera()
            } else {
                Toaster.error(this, "Permiso de cámara denegado")
            }
        }

    // Cámara (TakePicture)
    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && cameraImageUri != null) {
                pickedImage = cameraImageUri
                b.imgPhoto.load(cameraImageUri)
                Toaster.ok(this, "Foto capturada")
            } else {
                Toaster.info(this, "No se tomó la foto")
            }
        }

    // Galería (Photo Picker)
    private val pickPhoto =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                pickedImage = uri
                cameraImageUri = null
                b.imgPhoto.load(uri)
                Toaster.ok(this, "Imagen seleccionada")
            } else {
                Toaster.info(this, "No se seleccionó imagen")
            }
        }

    // ===== Lifecycle =====

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Botón: Galería
        b.btnSelectPhoto.setOnClickListener {
            pickPhoto.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        // Botón: Cámara
        b.btnTakePhoto.setOnClickListener {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

            if (granted) launchCamera()
            else requestCameraPermission.launch(Manifest.permission.CAMERA)
        }

        // Botón: Registrar
        b.btnRegister.setOnClickListener {
            val nombre = b.etNombre.text.toString().trim()
            val apellido = b.etApellido.text.toString().trim()
            val cedula = b.etCedula.text.toString().trim()
            val email = b.etEmail.text.toString().trim()
            val pass = b.etPassword.text.toString().trim()
            val latStr = b.etLat.text.toString().trim()
            val lngStr = b.etLng.text.toString().trim()

            // Validar entradas
            val latLng = validateInputs(
                nombre, apellido, cedula, email, pass, latStr, lngStr, pickedImage
            ) ?: return@setOnClickListener  // si falla, ya muestra toast y sale
            val (lat, lng) = latLng

            Toaster.info(this, "Creando cuenta...")

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    // 1) Registro Auth
                    repo.registerEmailPassword(email, pass)
                    val uid = repo.currentUid() ?: throw IllegalStateException("UID nulo")

                    // 2) Subir foto (no nulo garantizado por validación)
                    Toaster.info(this@RegisterActivity, "Subiendo foto...")
                    val photoUrl: String = pickedImage!!.let { uri ->
                        repo.uploadProfilePhotoHighRes(uid, uri)
                    }

                    // 3) Guardar perfil
                    Toaster.info(this@RegisterActivity, "Guardando perfil...")
                    val profile = UserProfile(
                        uid = uid,
                        nombre = nombre,
                        apellido = apellido,
                        email = email,
                        cedula = cedula,
                        photoUrl = photoUrl,
                        lat = lat,
                        lng = lng,
                        disponible = false
                    )
                    repo.saveUserProfile(profile)

                    // 4) Éxito → entrar al mapa (no volver al login)
                    Toaster.ok(this@RegisterActivity, "Usuario creado ✅")
                    startActivity(Intent(this@RegisterActivity, MainMapActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                    finish()

                } catch (e: Exception) {
                    val raw = e.message ?: "No se pudo completar el registro"
                    val msg = when {
                        "operation is not allowed" in raw.lowercase() ->
                            "Habilita Email/Password en Firebase"
                        "email address is already in use" in raw ->
                            "Ese correo ya está registrado"
                        "badly formatted" in raw.lowercase() || "invalid email" in raw.lowercase() ->
                            "Correo inválido"
                        "WEAK_PASSWORD" in raw || "Password should be at least" in raw ->
                            "La contraseña es muy débil"
                        else -> raw
                    }
                    Toaster.error(this@RegisterActivity, msg)
                }
            }
        }

        // Link: volver a Login (lo dejo por si lo usas en otra navegación)
        b.btnGoLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    // ===== Helpers =====

    private fun launchCamera() {
        // Carpeta cache/images (debe coincidir con file_paths.xml)
        val imagesDir = File(cacheDir, "images").apply { mkdirs() }
        val file = File.createTempFile("profile_", ".jpg", imagesDir)

        cameraImageUri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.fileprovider",
            file
        )

        cameraImageUri?.let { uri ->
            takePicture.launch(uri)
        } ?: Toaster.error(this, "No se pudo crear archivo temporal para la foto")
    }

    /**
     * Valida los campos y devuelve Pair(lat, lng) si todo está ok.
     * Si algo falla, muestra un toast y devuelve null.
     */
    private fun validateInputs(
        nombre: String,
        apellido: String,
        cedula: String,
        email: String,
        pass: String,
        latStr: String,
        lngStr: String,
        photo: Uri?
    ): Pair<Double, Double>? {

        if (nombre.isEmpty())  return fail("Falta el nombre")
        if (apellido.isEmpty()) return fail("Falta el apellido")
        if (cedula.isEmpty())  return fail("Falta la cédula")
        if (email.isEmpty())   return fail("Falta el correo")
        if (pass.isEmpty())    return fail("Falta la contraseña")
        if (latStr.isEmpty())  return fail("Latitud inválida (usa punto decimal)")
        if (lngStr.isEmpty())  return fail("Longitud inválida (usa punto decimal)")

        val lat = latStr.toDoubleOrNull() ?: return fail("Latitud inválida (usa punto decimal)")
        val lng = lngStr.toDoubleOrNull() ?: return fail("Longitud inválida (usa punto decimal)")

        // (Opcional) validar rangos razonables:
        if (lat !in -90.0..90.0)  return fail("Latitud fuera de rango (-90..90)")
        if (lng !in -180.0..180.0) return fail("Longitud fuera de rango (-180..180)")

        if (photo == null) return fail("Selecciona o toma una foto")

        return Pair(lat, lng)
    }

    private fun <T> fail(msg: String): T? {
        Toaster.error(this, msg)
        return null
    }
}
