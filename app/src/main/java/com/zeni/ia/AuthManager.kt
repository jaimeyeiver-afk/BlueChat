package com.zeni.ia

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import kotlinx.coroutines.tasks.await

/** El usuario cerró el selector de cuentas de Google sin elegir ninguna. */
class SignInCancelled : Exception()

/** No hay sesión iniciada (o ya no es válida). */
class NotSignedInException : Exception("Inicia sesión con Google para continuar.")

/**
 * Inicio de sesión con Google mediante Firebase Authentication.
 *
 * La configuración (proyecto Firebase y "ID de cliente web") llega en google-services.json,
 * que el plugin de Google Services convierte en recursos al compilar. Nada de eso está escrito aquí.
 */
class AuthManager(context: Context) {
    private val appContext = context.applicationContext

    /** Lo genera el plugin de Google Services cuando Google está activado como proveedor en Firebase. */
    private val webClientId: String? = run {
        val id = appContext.resources.getIdentifier("default_web_client_id", "string", appContext.packageName)
        if (id == 0) null else appContext.getString(id).takeIf { it.isNotBlank() }
    }

    /** `false` mientras la APK se compile sin google-services.json (Firebase sin configurar). */
    val isConfigured: Boolean
        get() = FirebaseApp.getApps(appContext).isNotEmpty() && webClientId != null

    private val auth: FirebaseAuth?
        get() = if (isConfigured) FirebaseAuth.getInstance() else null

    /** Avisa al instante y en cada cambio de sesión. Devuelve una función para dejar de escuchar. */
    fun observe(onChange: (FirebaseUser?) -> Unit): () -> Unit {
        val fa = auth ?: return {}
        val listener = FirebaseAuth.AuthStateListener { onChange(it.currentUser) }
        fa.addAuthStateListener(listener)
        return { fa.removeAuthStateListener(listener) }
    }

    suspend fun signIn(activity: Activity) {
        val fa = auth ?: error("Firebase todavía no está configurado en esta versión de la app.")
        val clientId = webClientId ?: error("Falta el ID de cliente web de Google en la configuración de Firebase.")
        try {
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(GetSignInWithGoogleOption.Builder(clientId).build())
                .build()
            val credential = CredentialManager.create(activity).getCredential(activity, request).credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val google = GoogleIdTokenCredential.createFrom(credential.data)
                fa.signInWithCredential(GoogleAuthProvider.getCredential(google.idToken, null)).await()
            } else {
                error("Tipo de credencial no compatible.")
            }
        } catch (e: GetCredentialCancellationException) {
            throw SignInCancelled()
        } catch (e: GetCredentialException) {
            // Teléfonos antiguos (como el J4 Core) a veces no pueden usar el selector de cuentas:
            // probamos el inicio de sesión de Google por navegador, también con Firebase.
            fa.startActivityForSignInWithProvider(activity, OAuthProvider.newBuilder("google.com").build()).await()
        }
    }

    /** Si Android cerró la app mientras se abría el navegador, recupera el resultado pendiente. */
    suspend fun resumePendingBrowserSignIn() {
        try {
            auth?.pendingAuthResult?.await()
        } catch (_: Exception) {
            // Sin resultado pendiente o falló: el usuario puede volver a intentarlo.
        }
    }

    /** Token de sesión que el servidor verifica. Firebase lo renueva solo cuando vence. */
    suspend fun idToken(forceRefresh: Boolean = false): String {
        val user = auth?.currentUser ?: throw NotSignedInException()
        return user.getIdToken(forceRefresh).await().token ?: throw NotSignedInException()
    }

    suspend fun signOut() {
        auth?.signOut()
        try {
            CredentialManager.create(appContext).clearCredentialState(ClearCredentialStateRequest())
        } catch (_: Exception) {
            // No es crítico: la sesión de Firebase ya se cerró.
        }
    }
}
