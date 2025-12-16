package com.example.roommade.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.roommade.auth.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseUser

@Composable
fun LoginScreen(
    vm: AuthViewModel,
    onSignedIn: (FirebaseUser) -> Unit
    ) {
    val user by vm.user.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error by vm.error.collectAsState()
    val context = LocalContext.current

    val signInClient = rememberGoogleClient()
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val token = account?.idToken
            if (token != null) {
                vm.signInWithGoogle(token)
            } else {
                vm.error.value = "구글 토큰을 가져오지 못했습니다."
            }
        } catch (e: ApiException) {
            vm.error.value = e.message
        }
    }

    LaunchedEffect(user) {
        user?.let { onSignedIn(it) }
    }

    SignInCard(
        title = "RoomMade",
        onClick = { launcher.launch(signInClient.signInIntent) },
        isLoading = isLoading,
        error = error
    )
}

@Composable
private fun rememberGoogleClient() = run {
    val context = LocalContext.current
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(context.getString(com.example.roommade.R.string.default_web_client_id))
        .requestEmail()
        .build()
    androidx.compose.runtime.remember(context) { GoogleSignIn.getClient(context, gso) }
}

@Composable
fun LoginDialog(
    vm: AuthViewModel,
    onSignedIn: (FirebaseUser) -> Unit,
    onDismiss: () -> Unit
) {
    val user by vm.user.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error by vm.error.collectAsState()
    val signInClient = rememberGoogleClient()
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val token = account?.idToken
            if (token != null) {
                vm.signInWithGoogle(token)
            } else {
                vm.error.value = "구글 토큰을 가져오지 못했습니다."
            }
        } catch (e: ApiException) {
            vm.error.value = e.message
        }
    }

    LaunchedEffect(user) {
        user?.let { onSignedIn(it) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            tonalElevation = 8.dp,
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "RoomMade 로그인",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Google 계정으로 빠르게 시작하세요.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    textAlign = TextAlign.Center
                )
                GoogleStyledButton(
                    onClick = { launcher.launch(signInClient.signInIntent) },
                    enabled = !isLoading
                )
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(top = 4.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                ) {
                    Text("닫기")
                }
            }
        }
    }
}

@Composable
private fun SignInCard(
    title: String?,
    onClick: () -> Unit,
    isLoading: Boolean,
    error: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            title?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            GoogleSignInButton(
                onClick = onClick,
                enabled = !isLoading
            )
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun GoogleStyledButton(
    onClick: () -> Unit,
    enabled: Boolean
) {
    GoogleSignInButton(onClick = onClick, enabled = enabled)
}

@Composable
private fun GoogleSignInButton(onClick: () -> Unit, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFF5F5F5),
            contentColor = Color(0xFF202124)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(id = com.example.roommade.R.drawable.ic_google_logo),
                contentDescription = "Google logo",
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Google로 계속하기",
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
