package com.minami_studio.kiro.ui.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.minami_studio.kiro.ui.theme.*
import com.minami_studio.kiro.util.Strings
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@Composable
fun RestoreSubscriptionDialog(
    strings: Strings,
    onSendCode: suspend (String) -> String?,
    onVerify: suspend (String, String) -> Boolean,
    onForceUnbind: suspend (String) -> Boolean,
    onDismiss: () -> Unit
) {
    var step by remember { mutableStateOf(0) }
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }
    var showForceUnbind by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WanderWarm)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(WanderBlush)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, strings.close, tint = WanderInk, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(48.dp))

                Icon(Icons.Default.Restore, null, tint = WanderAccent, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(24.dp))
                Text(strings.subRestoreTitle, fontSize = 24.sp, fontFamily = FontFamily.Serif, color = WanderInk)
                Spacer(modifier = Modifier.height(8.dp))

                when {
                    success -> {
                        Spacer(modifier = Modifier.height(32.dp))
                        Icon(Icons.Default.CheckCircle, null, tint = WanderAccent, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(strings.subRestoreSuccess, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = WanderInk)
                    }
                    step == 0 -> {
                        Text(
                            strings.subRestoreDesc,
                            fontSize = 14.sp, color = WanderMuted, textAlign = TextAlign.Center, lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it; error = null },
                            label = { Text(strings.subEmailPlaceholder) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                            shape = RoundedCornerShape(12.dp),
                            isError = error != null
                        )
                        if (error != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(error!!, fontSize = 13.sp, color = Color.Red, textAlign = TextAlign.Center)
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                if (email.isBlank() || !email.contains("@")) {
                                    error = strings.subInvalidEmail
                                    return@Button
                                }
                                isLoading = true; error = null
                                GlobalScope.launch {
                                    try {
                                        val errorMsg = onSendCode(email)
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            isLoading = false
                                            if (errorMsg == null) step = 1 else error = errorMsg
                                        }
                                    } catch (e: Exception) {
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            isLoading = false
                                            error = e.message ?: strings.subRestoreFailed
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            enabled = !isLoading && email.isNotBlank(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WanderInk, contentColor = WanderCream)
                        ) {
                            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = WanderCream, strokeWidth = 2.dp)
                            else Text(strings.subSendCode, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = WanderCream)
                        }
                    }
                    step == 1 -> {
                        Text(strings.subRestoreDesc, fontSize = 14.sp, color = WanderMuted, textAlign = TextAlign.Center, lineHeight = 20.sp)
                        Spacer(modifier = Modifier.height(32.dp))
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it; error = null },
                            label = { Text(strings.subCodePlaceholder) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            shape = RoundedCornerShape(12.dp),
                            isError = error != null
                        )
                        if (error != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(error!!, fontSize = 13.sp, color = Color.Red, textAlign = TextAlign.Center)
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                if (code.length != 4) { error = strings.subInvalidCode; return@Button }
                                isLoading = true; error = null
                                GlobalScope.launch {
                                    try {
                                        val result = onVerify(email, code)
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            isLoading = false
                                            if (result) success = true else { error = strings.subRestoreFailed; showForceUnbind = true }
                                        }
                                    } catch (e: Exception) {
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            isLoading = false
                                            error = e.message ?: strings.subRestoreFailed
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            enabled = !isLoading && code.length == 4,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WanderInk)
                        ) {
                            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = WanderCream, strokeWidth = 2.dp)
                            else Text(strings.subVerify, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = WanderCream)
                        }
                        if (showForceUnbind) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(strings.subForceUnlink, fontSize = 14.sp, color = WanderAccent, fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable { step = 2; error = null })
                        }
                    }
                    step == 2 -> {
                        Text(strings.subForceUnlinkDesc, fontSize = 14.sp, color = WanderMuted, textAlign = TextAlign.Center, lineHeight = 20.sp)
                        Spacer(modifier = Modifier.height(32.dp))
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it; error = null },
                            label = { Text(strings.subCodePlaceholder) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            shape = RoundedCornerShape(12.dp),
                            isError = error != null
                        )
                        if (error != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(error!!, fontSize = 13.sp, color = Color.Red, textAlign = TextAlign.Center)
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                if (code.length != 4) { error = strings.subInvalidCode; return@Button }
                                isLoading = true; error = null
                                GlobalScope.launch {
                                    try {
                                        val result = onForceUnbind(email)
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            isLoading = false
                                            if (result) { step = 0; code = ""; error = null } else error = strings.subRestoreFailed
                                        }
                                    } catch (e: Exception) {
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            isLoading = false
                                            error = e.message ?: strings.subRestoreFailed
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            enabled = !isLoading && code.length == 4,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))
                        ) {
                            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            else Text(strings.subForceUnlink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
