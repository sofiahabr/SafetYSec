package com.example.safetysec.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safetysec.domain.model.AuthResult
import com.example.safetysec.domain.model.User
import com.example.safetysec.domain.usecase.auth.GetCurrentUserUseCase
import com.example.safetysec.domain.usecase.auth.LoginUseCase
import com.example.safetysec.domain.usecase.auth.LogoutUseCase
import com.example.safetysec.domain.usecase.auth.RegisterUseCase
import com.example.safetysec.domain.usecase.auth.UpdateProfileUseCase
import com.example.safetysec.domain.usecase.auth.ChangePasswordUseCase
import com.example.safetysec.domain.usecase.auth.DeleteUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.Job
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.util.Log
import com.example.safetysec.domain.repository.AuthRepository
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneMultiFactorAssertion
import java.util.concurrent.TimeUnit
import com.google.firebase.auth.FirebaseAuth

data class AuthState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: String? = null,
    val isAuthenticated: Boolean = false,
    val registrationSuccess: Boolean = false,
    val mfaRequired: Boolean = false,
    val verificationId: String? = null,
    val mfaPhoneNumber: String? = null,
    val passwordResetSuccess: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val changePasswordUseCase: ChangePasswordUseCase,
    private val deleteUserProfileUseCase: DeleteUserProfileUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private var currentUserJob: Job? = null

    init {
        checkCurrentUser()
    }

    fun updateProfile(
        email: String,
        name: String,
        phone: String,
        role: String
    ) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            val result = updateProfileUseCase(email, name, phone, role)
            when (result) {
                is AuthResult.Success -> {
                    _authState.value = _authState.value.copy(
                        user = result.data,
                        isLoading = false
                    )
                }

                is AuthResult.Error -> {
                    _authState.value = _authState.value.copy(
                        error = result.message,
                        isLoading = false
                    )
                }

                is AuthResult.Loading -> {
                    _authState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            val result = changePasswordUseCase(currentPassword, newPassword)
            when (result) {
                is AuthResult.Success -> {
                    _authState.value = _authState.value.copy(
                        user = result.data,
                        isLoading = false
                    )
                }

                is AuthResult.Error -> {
                    _authState.value = _authState.value.copy(
                        error = result.message,
                        isLoading = false
                    )
                }

                else -> {}
            }
        }
    }

    fun login(email: String, password: String, activity: android.app.Activity? = null) {
        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, error = null) }

            try {
                val auth = FirebaseAuth.getInstance()

                auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener { authResult ->
                        // Basic login successful, no MFA enrolled
                        Log.d("AuthViewModel", "Login successful, no MFA required")
                        _authState.update {
                            it.copy(
                                isLoading = false,
                                user = authResult.user?.let { user ->
                                    User(
                                        id = user.uid,
                                        email = user.email ?: "",
                                        name = user.displayName ?: "",
                                        alertCancellationCode = null
                                    )
                                },
                                isAuthenticated = true,
                                mfaRequired = false
                            )
                        }
                    }
                    .addOnFailureListener { exception ->
                        when (exception) {
                            is com.google.firebase.auth.FirebaseAuthMultiFactorException -> {
                                Log.d("AuthViewModel", "MFA required during login")
                                try {
                                    val multiFactorResolver = exception.resolver
                                    val hints = multiFactorResolver.hints

                                    // Store the resolver for later use in verifyMFACode
                                    setMFAResolver(multiFactorResolver)
                                    Log.d("AuthViewModel", "MFA resolver stored for login verification")

                                    Log.d("AuthViewModel", "MFA hints available: ${hints.size}")

                                    // Find phone factor
                                    val phoneFactorHint = hints.find {
                                        it is com.google.firebase.auth.PhoneMultiFactorInfo
                                    }

                                    if (phoneFactorHint != null) {
                                        val phoneInfo = phoneFactorHint as com.google.firebase.auth.PhoneMultiFactorInfo
                                        var phoneNumber = phoneInfo.phoneNumber

                                        // Ensure phone number is in E.164 format: +[country code][number]
                                        if (!phoneNumber.startsWith("+")) {
                                            phoneNumber = "+$phoneNumber"
                                        }

                                        Log.d("AuthViewModel", "MFA Phone: $phoneNumber")

                                        // Send verification code to the phone number
                                        Log.d("AuthViewModel", "Sending verification code to $phoneNumber")
                                        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                                            override fun onVerificationCompleted(credential: com.google.firebase.auth.PhoneAuthCredential) {
                                                Log.d("AuthViewModel", "Auto-retrieved SMS code during MFA login")
                                            }

                                            override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                                                Log.e("AuthViewModel", "MFA code send failed: ${e.message}", e)
                                                _authState.update { it.copy(
                                                    isLoading = false,
                                                    error = "Failed to send verification code: ${e.message}"
                                                ) }
                                            }

                                            override fun onCodeSent(
                                                verificationId: String,
                                                token: PhoneAuthProvider.ForceResendingToken
                                            ) {
                                                Log.d("AuthViewModel", "✓ Verification code sent successfully for MFA login")
                                                Log.d("AuthViewModel", "New Verification ID: $verificationId")
                                                _authState.update { it.copy(
                                                    isLoading = false,
                                                    mfaRequired = true,
                                                    verificationId = verificationId,
                                                    mfaPhoneNumber = phoneNumber,
                                                    error = null
                                                ) }
                                            }
                                        }

                                        if (activity != null) {

                                            val options = PhoneAuthOptions.newBuilder(auth)
                                                .setMultiFactorHint(phoneFactorHint)
                                                .setMultiFactorSession(multiFactorResolver.session)
                                                .setTimeout(60L, TimeUnit.SECONDS)
                                                .setActivity(activity)
                                                .setCallbacks(callbacks)
                                                .build()



                                            PhoneAuthProvider.verifyPhoneNumber(options)
                                        } else {
                                            Log.e("AuthViewModel", "Activity not provided for MFA login")
                                            _authState.update { it.copy(
                                                isLoading = false,
                                                error = "Activity required for MFA verification"
                                            ) }
                                        }

                                    } else {
                                        _authState.update {
                                            it.copy(
                                                isLoading = false,
                                                error = "No MFA method found"
                                            )
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("AuthViewModel", "Failed to handle MFA during login", e)
                                    _authState.update {
                                        it.copy(
                                            isLoading = false,
                                            error = "MFA verification failed: ${e.message}"
                                        )
                                    }
                                }
                            }
                            is com.google.firebase.auth.FirebaseAuthInvalidUserException -> {
                                _authState.update {
                                    it.copy(
                                        isLoading = false,
                                        error = "Account not found"
                                    )
                                }
                            }
                            is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> {
                                _authState.update {
                                    it.copy(
                                        isLoading = false,
                                        error = "Invalid email or password"
                                    )
                                }
                            }
                            else -> {
                                _authState.update {
                                    it.copy(
                                        isLoading = false,
                                        error = exception.message ?: "Login failed"
                                    )
                                }
                            }
                        }
                    }

            } catch (e: Exception) {
                Log.e("AuthViewModel", "Login failed", e)
                _authState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Login failed"
                    )
                }
            }
        }
    }
    fun register(email: String, password: String, name: String, phone: String, role: String) {
        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, error = null) }

            val result = registerUseCase(email, password, name, phone, role)

            when (result) {
                is AuthResult.Success -> {
                    _authState.update {
                        it.copy(
                            isLoading = false,
                            user = result.data,
                            isAuthenticated = true,
                            registrationSuccess = true
                        )
                    }

                    result.data?.id?.let { userId ->
                        registerFCMToken(userId)
                    }
                }

                is AuthResult.Error -> {
                    _authState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }

                is AuthResult.Loading -> {
                    _authState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                logoutUseCase()
                _authState.update {
                    it.copy(
                        isLoading = false,
                        user = null,
                        isAuthenticated = false,
                        error = null
                    )
                }

                currentUserJob?.cancel()
                currentUserJob = null
            } catch (e: Exception) {
                _authState.update {
                    it.copy(
                        isLoading = false,
                        error = "Logout failed"
                    )
                }
            }
        }
    }

    private fun checkCurrentUser() {
        currentUserJob?.cancel()
        currentUserJob = viewModelScope.launch {
            try {
                // If GetCurrentUserUseCase returns Flow<User?>
                getCurrentUserUseCase().collect { currentUser ->
                    _authState.update {
                        it.copy(
                            user = currentUser,
                            isAuthenticated = currentUser != null
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Failed to check current user", e)
                _authState.update {
                    it.copy(
                        user = null,
                        isAuthenticated = false
                    )
                }
            }
        }
    }

    private fun registerFCMToken(userId: String) {
        viewModelScope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()

                val db = FirebaseFirestore.getInstance()
                db.collection("users").document(userId).update("fcmToken", token).await()

                Log.d("AuthViewModel", "FCM token registered successfully for user: $userId")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Failed to register FCM token", e)
            }
        }
    }

    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, error = null) }

            try {
                FirebaseAuth.getInstance().sendPasswordResetEmail(email).await()

                _authState.update {
                    it.copy(
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("There is no user") == true -> {
                        "No account found with this email address"
                    }

                    e.message?.contains("too many requests") == true -> {
                        "Too many reset requests. Please try again later."
                    }

                    else -> {
                        e.message ?: "Failed to send reset email"
                    }
                }

                _authState.update {
                    it.copy(
                        isLoading = false,
                        error = errorMessage
                    )
                }
            }
        }
    }

    fun resetPassword(code: String, newPassword: String) {
        viewModelScope.launch {
            _authState.update {
                it.copy(
                    isLoading = true,
                    error = null
                )
            }

            try {
                FirebaseAuth.getInstance()
                    .confirmPasswordReset(code, newPassword)
                    .await()

                _authState.update {
                    it.copy(
                        isLoading = false,
                        error = null,
                        passwordResetSuccess = true
                    )
                }
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("password is invalid") == true -> {
                        "Password must be at least 6 characters"
                    }

                    e.message?.contains("code is invalid") == true -> {
                        "Reset link has expired. Please request a new one."
                    }

                    else -> {
                        e.message ?: "Failed to reset password"
                    }
                }

                _authState.update {
                    it.copy(
                        isLoading = false,
                        error = errorMessage
                    )
                }
            }
        }
    }

    fun verifyPasswordResetCode(code: String, onSuccess: (Boolean) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                FirebaseAuth.getInstance()
                    .verifyPasswordResetCode(code)
                onSuccess(true)
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("code is invalid") == true -> {
                        "Invalid or expired reset code"
                    }
                    else -> {
                        e.message ?: "Failed to verify code"
                    }
                }
                onError(errorMessage)
            }
        }
    }

    fun clearPasswordRecoveryError() {
        _authState.update { it.copy(error = null, passwordResetSuccess = false) }
    }

    // Enhanced sendMFACode function to handle E.164 format properly
// Replace your current sendMFACode function in AuthViewModel.kt with this:

    fun sendMFACode(phoneNumber: String, activity: android.app.Activity) {
        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, error = null) }

            try {
                Log.d("AuthViewModel", "📞 Starting phone verification for: '$phoneNumber'")

                // Step 1: Clean the phone number - remove all non-digit characters except +
                val cleaned = phoneNumber
                    .replace(Regex("[\\s\\-().]"), "")  // Remove spaces, dashes, parens, dots
                    .trim()

                Log.d("AuthViewModel", "Cleaned phone: '$phoneNumber' → '$cleaned'")

                // Step 2: Ensure international format (starts with +)
                val international = when {
                    cleaned.startsWith("+") -> {
                        // Already has +, use as-is
                        cleaned
                    }
                    cleaned.startsWith("00") -> {
                        // Handle 0047 format (0047 = +47)
                        "+" + cleaned.substring(2)
                    }
                    cleaned.startsWith("0") -> {
                        // Handle 046652190 → +4746652190 (Norwegian format)
                        // Default to +47 if no country code
                        "+47" + cleaned.substring(1)
                    }
                    else -> {
                        // No leading 0 or +, check if it looks like just the subscriber number
                        if (cleaned.length <= 8) {
                            // Likely just subscriber, add +47 (Norway)
                            "+47$cleaned"
                        } else {
                            // Assume it has country code but missing +
                            "+$cleaned"
                        }
                    }
                }

                Log.d("AuthViewModel", "International format: '$international'")

                // Step 3: Validate E.164 format
                if (!international.startsWith("+")) {
                    _authState.update { it.copy(
                        isLoading = false,
                        error = "Phone must start with + (E.164 format required)"
                    ) }
                    return@launch
                }

                val digitsOnly = international.drop(1)

                if (!digitsOnly.all { it.isDigit() }) {
                    _authState.update { it.copy(
                        isLoading = false,
                        error = "Phone contains invalid characters. Use format: +4746652190"
                    ) }
                    return@launch
                }

                if (digitsOnly.length < 7 || digitsOnly.length > 15) {
                    _authState.update { it.copy(
                        isLoading = false,
                        error = "Phone must have 7-15 digits (got ${digitsOnly.length}). Format: +4746652190"
                    ) }
                    return@launch
                }

                Log.d("AuthViewModel", "✅ Phone validation passed: $international")
                Log.d("AuthViewModel", "   Country code: ${digitsOnly.take(2)}, Subscriber: ${digitsOnly.drop(2)}")

                // Step 4: Create callbacks for verification
                val auth = FirebaseAuth.getInstance()
                val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(
                        credential: com.google.firebase.auth.PhoneAuthCredential
                    ) {
                        Log.d("AuthViewModel", "✓ Auto-retrieved SMS code: ${credential.smsCode}")
                        // Don't auto-verify - let user enter code manually for security
                    }

                    override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                        Log.e("AuthViewModel", "❌ Verification failed: ${e.message}", e)
                        e.printStackTrace()

                        val errorMessage = when {
                            e.message?.contains("Invalid format") == true -> {
                                "Phone format error. Use E.164 format: +4746652190"
                            }
                            e.message?.contains("not configured") == true -> {
                                "Phone authentication not configured in Firebase Console"
                            }
                            e is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> {
                                "Invalid phone number. Check if test numbers are configured in Firebase Console"
                            }
                            e.message?.contains("too many attempts") == true -> {
                                "Too many verification attempts. Please wait 15 minutes"
                            }
                            e.message?.contains("invalid phone") == true -> {
                                "Phone number not valid. Check if it's registered as a test number in Firebase Console"
                            }
                            else -> {
                                "Verification failed: ${e.message ?: "Unknown error"}"
                            }
                        }

                        Log.e("AuthViewModel", "Error message: $errorMessage")

                        _authState.update { it.copy(
                            isLoading = false,
                            error = errorMessage
                        ) }
                    }

                    override fun onCodeSent(
                        verificationId: String,
                        token: PhoneAuthProvider.ForceResendingToken
                    ) {
                        Log.d("AuthViewModel", "✓✓✓ Verification code sent successfully")
                        Log.d("AuthViewModel", "     Phone: $international")
                        Log.d("AuthViewModel", "     Verification ID: $verificationId")
                        _authState.update { it.copy(
                            isLoading = false,
                            verificationId = verificationId,
                            mfaPhoneNumber = international,
                            error = null
                        ) }
                    }
                }

                // Step 5: Create phone auth options and send verification code
                Log.d("AuthViewModel", "📱 Sending verification to: $international")

                val options = PhoneAuthOptions.newBuilder(auth)
                    .setPhoneNumber(international)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(activity)
                    .setCallbacks(callbacks)
                    .build()

                Log.d("AuthViewModel", "📞 Calling PhoneAuthProvider.verifyPhoneNumber()")
                PhoneAuthProvider.verifyPhoneNumber(options)

            } catch (e: Exception) {
                Log.e("AuthViewModel", "⚠️ Exception in sendMFACode: ${e.message}", e)
                e.printStackTrace()
                _authState.update { it.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to send verification code"
                ) }
            }
        }
    }

    // Helper function to validate E.164 format (optional, for testing)
    private fun isValidE164Phone(phone: String): Boolean {
        val e164Regex = Regex("^\\+[1-9]\\d{1,14}$")
        return e164Regex.matches(phone)
    }

    fun verifyMFASetupCode(code: String) {
        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, error = null) }

            try {
                val verificationId = _authState.value.verificationId
                    ?: throw Exception("Verification ID not found")

                val credential = PhoneAuthProvider.getCredential(
                    verificationId,
                    code
                )

                val auth = FirebaseAuth.getInstance()
                val user = auth.currentUser
                    ?: throw Exception("User not authenticated")

                val multiFactorAssertion = PhoneMultiFactorAssertion(credential)
                user.multiFactor.enroll(multiFactorAssertion, null)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d("AuthViewModel", "MFA setup successful")
                            _authState.update { it.copy(
                                isLoading = false,
                                error = null,
                                verificationId = null,
                                mfaPhoneNumber = null
                            ) }
                        } else {
                            Log.e("AuthViewModel", "MFA setup failed", task.exception)
                            _authState.update { it.copy(
                                isLoading = false,
                                error = task.exception?.message ?: "Verification failed"
                            ) }
                        }
                    }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Exception in verifyMFASetupCode", e)
                _authState.update { it.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to verify code"
                ) }
            }
        }
    }

    // Store the resolver for later use
    private var mfaResolver: com.google.firebase.auth.MultiFactorResolver? = null

    fun setMFAResolver(resolver: com.google.firebase.auth.MultiFactorResolver?) {
        mfaResolver = resolver
    }

    fun verifyMFACode(code: String) {
        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, error = null) }

            try {
                Log.d("AuthViewModel", "Starting MFA code verification...")

                val verificationId = _authState.value.verificationId
                    ?: throw Exception("Verification ID not found")

                Log.d("AuthViewModel", "Verification ID found")

                val credential = com.google.firebase.auth.PhoneAuthProvider.getCredential(
                    verificationId,
                    code
                )

                Log.d("AuthViewModel", "Phone credential created")

                val resolver = mfaResolver
                Log.d("AuthViewModel", "MFA Resolver: ${if (resolver != null) "SET (Login flow)" else "NULL (Setup flow)"}")

                if (resolver != null) {
                    // This is a login MFA verification
                    Log.d("AuthViewModel", "Using resolver for login MFA verification")
                    val assertion = com.google.firebase.auth.PhoneMultiFactorAssertion(credential)
                    resolver.resolveSignIn(assertion)
                        .addOnSuccessListener { authResult ->
                            Log.d("AuthViewModel", "✓ MFA login verification successful")
                            _authState.update { it.copy(
                                isLoading = false,
                                mfaRequired = false,
                                isAuthenticated = true,
                                user = authResult.user?.let { user ->
                                    User(
                                        id = user.uid,
                                        email = user.email ?: "",
                                        name = user.displayName ?: "",
                                        alertCancellationCode = null
                                    )
                                },
                                error = null,
                                verificationId = null
                            ) }
                            mfaResolver = null
                        }
                        .addOnFailureListener { e ->
                            Log.e("AuthViewModel", "❌ MFA resolution failed: ${e.message}", e)
                            _authState.update { it.copy(
                                isLoading = false,
                                error = when {
                                    e.message?.contains("invalid code") == true -> "Invalid code. Please try again."
                                    e.message?.contains("invalid credential") == true -> "Invalid verification code."
                                    else -> e.message ?: "Failed to verify code"
                                }
                            ) }
                        }
                        .await()
                } else {
                    // This is a setup MFA verification (during registration)
                    Log.d("AuthViewModel", "No resolver found - using setup flow for MFA enrollment")
                    val auth = FirebaseAuth.getInstance()
                    val user = auth.currentUser
                        ?: throw Exception("User not authenticated for MFA setup")

                    Log.d("AuthViewModel", "Current user: ${user.uid}")

                    val multiFactorAssertion = com.google.firebase.auth.PhoneMultiFactorAssertion(credential)
                    user.multiFactor.enroll(multiFactorAssertion, null)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Log.d("AuthViewModel", "✓ MFA setup verification successful")
                                _authState.update { it.copy(
                                    isLoading = false,
                                    error = null,
                                    verificationId = null,
                                    mfaPhoneNumber = null
                                ) }
                            } else {
                                Log.e("AuthViewModel", "❌ MFA setup failed: ${task.exception?.message}", task.exception)
                                _authState.update { it.copy(
                                    isLoading = false,
                                    error = task.exception?.message ?: "Verification failed"
                                ) }
                            }
                        }
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "❌ Exception in verifyMFACode: ${e.message}", e)
                _authState.update { it.copy(
                    isLoading = false,
                    error = when {
                        e.message?.contains("invalid code") == true -> "Invalid code. Please try again."
                        e.message?.contains("User not authenticated") == true -> "Please login again to set up MFA"
                        else -> e.message ?: "Failed to verify code"
                    }
                ) }
            }
        }
    }

    fun clearMFAState() {
        _authState.update { it.copy(
            mfaRequired = false,
            verificationId = null,
            mfaPhoneNumber = null
        ) }
    }

    fun clearError() {
        _authState.update { it.copy(error = null) }
    }

    fun updateCancellationPin(newPin: String, onResult: (success: Boolean, error: String?) -> Unit) {
        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, error = null) }

            try {
                // Validate PIN format
                if (newPin.length != 4 || !newPin.all { it.isDigit() }) {
                    onResult(false, "PIN must be exactly 4 digits")
                    _authState.update { it.copy(
                        isLoading = false,
                        error = "PIN must be exactly 4 digits"
                    ) }
                    return@launch
                }

                // TODO: Implement using your UpdateCancellationPinUseCase
                // val result = updateCancellationPinUseCase(newPin)

                // For now, assume success
                _authState.update { it.copy(
                    isLoading = false,
                    error = null
                ) }

                onResult(true, null)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Failed to update cancellation PIN", e)
                val errorMsg = e.message ?: "Failed to update cancellation PIN"
                _authState.update { it.copy(
                    isLoading = false,
                    error = errorMsg
                ) }
                onResult(false, errorMsg)
            }
        }
    }

    fun deleteUserProfile() {
        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, error = null) }

            try {
                Log.d("AuthViewModel", "Starting profile deletion...")
                val result = deleteUserProfileUseCase.invoke()

                Log.d("AuthViewModel", "Delete result type: ${result.javaClass.simpleName}")

                when (result) {
                    is AuthResult.Success -> {
                        Log.d("AuthViewModel", "Profile deleted successfully")
                        _authState.update { it.copy(
                            isLoading = false,
                            user = null,
                            isAuthenticated = false,
                            error = null
                        ) }
                        // Also logout from Firebase
                        try {
                            FirebaseAuth.getInstance().signOut()
                            Log.d("AuthViewModel", "Firebase sign out completed")
                        } catch (e: Exception) {
                            Log.e("AuthViewModel", "Failed to sign out from Firebase", e)
                        }
                    }
                    is AuthResult.Error -> {
                        Log.e("AuthViewModel", "Profile deletion error: ${result.message}")
                        _authState.update { it.copy(
                            isLoading = false,
                            error = result.message ?: "Failed to delete profile"
                        ) }
                    }
                    is AuthResult.Loading -> {
                        Log.d("AuthViewModel", "Profile deletion in progress...")
                        _authState.update { it.copy(isLoading = true) }
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Exception in deleteUserProfile", e)
                e.printStackTrace()
                _authState.update { it.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to delete profile"
                ) }
            }
        }
    }
}