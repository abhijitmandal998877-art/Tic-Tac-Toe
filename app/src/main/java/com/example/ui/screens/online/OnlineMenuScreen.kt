package com.example.ui.screens.online

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.online.model.OnlineUser
import com.example.ui.components.GameBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun OnlineMenuScreen(
    user: OnlineUser?,
    isLoading: Boolean,
    errorMessage: String?,
    onQuickMatch: () -> Unit,
    onCreateRoom: () -> Unit,
    onOpenJoinDialog: () -> Unit,
    onUpdateDisplayName: (String) -> Unit,
    onBackToHome: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showEditNameDialog by remember { mutableStateOf(false) }
    var editedName by remember(user?.displayName) { mutableStateOf(user?.displayName ?: "") }

    GameBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackToHome,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant.copy(alpha = 0.8f))
                        .testTag("online_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Home",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "ONLINE MULTIPLAYER",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Play with friends anywhere in real-time",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Player Profile Card
            PlayerProfileCard(
                user = user,
                onEditNameClick = {
                    editedName = user?.displayName ?: ""
                    showEditNameDialog = true
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action: QUICK MATCH (⚡ Auto Matchmaking)
            QuickMatchCard(
                isLoading = isLoading,
                onClick = onQuickMatch
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action: CREATE ROOM
            CreateRoomCard(
                isLoading = isLoading,
                onClick = onCreateRoom
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action: JOIN ROOM
            JoinRoomCard(
                isLoading = isLoading,
                onClick = onOpenJoinDialog
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Connection Status Info Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF131C2E))
                    .border(1.dp, Color(0xFF233550), RoundedCornerShape(12.dp))
                    .padding(vertical = 10.dp, horizontal = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (user != null) Icons.Default.Wifi else Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = if (user != null) NeonEmerald else NeonPink,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (user != null) "Connected to Firebase Realtime Engine" else "Connecting...",
                        fontSize = 12.sp,
                        color = if (user != null) TextSecondary else NeonPink
                    )
                }
            }
        }
    }

    // Edit Name Dialog
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = {
                Text(
                    text = "Edit Display Name",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Choose your public gaming nickname visible to opponents:",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { if (it.length <= 20) editedName = it },
                        singleLine = true,
                        placeholder = { Text("e.g. MasterGamer") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color(0xFF384D72),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("edit_display_name_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEditNameDialog = false
                        if (editedName.isNotBlank()) {
                            onUpdateDisplayName(editedName)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = Color(0xFF06151A)
                    ),
                    modifier = Modifier.testTag("save_display_name_button")
                ) {
                    Text("SAVE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEditNameDialog = false }) {
                    Text("CANCEL")
                }
            },
            containerColor = Color(0xFF162136),
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Error Dialog
    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = onDismissError,
            title = {
                Text(
                    text = "Notice",
                    fontWeight = FontWeight.Bold,
                    color = NeonPink
                )
            },
            text = {
                Text(text = errorMessage, color = TextPrimary, fontSize = 14.sp)
            },
            confirmButton = {
                Button(
                    onClick = onDismissError,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF090D16))
                ) {
                    Text("OK")
                }
            },
            containerColor = Color(0xFF162136),
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun PlayerProfileCard(
    user: OnlineUser?,
    onEditNameClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(DarkSurface.copy(alpha = 0.9f))
            .border(1.dp, Color(0xFF283854), shape)
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(NeonPurple.copy(alpha = 0.2f))
                            .border(1.5.dp, NeonPurple, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = NeonPurple,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column {
                        Text(
                            text = user?.displayName ?: "Guest Player",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Online Profile",
                            fontSize = 12.sp,
                            color = NeonEmerald
                        )
                    }
                }

                IconButton(
                    onClick = onEditNameClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E2B42))
                        .testTag("edit_name_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Name",
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatBadge(
                    label = "WINS",
                    value = user?.wins ?: 0,
                    color = NeonCyan,
                    modifier = Modifier.weight(1f)
                )
                StatBadge(
                    label = "LOSSES",
                    value = user?.losses ?: 0,
                    color = NeonPink,
                    modifier = Modifier.weight(1f)
                )
                StatBadge(
                    label = "DRAWS",
                    value = user?.draws ?: 0,
                    color = NeonAmber,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatBadge(
    label: String,
    value: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF141E30))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = value.toString(), fontSize = 18.sp, fontWeight = FontWeight.Black, color = TextPrimary)
        }
    }
}

@Composable
private fun CreateRoomCard(
    isLoading: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .shadow(12.dp, shape, spotColor = NeonCyan)
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF0284C7), Color(0xFF0369A1))
                )
            )
            .clickable(enabled = !isLoading, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("create_room_card"),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircleOutline,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "CREATE ROOM",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = "Generate a 6-digit code and invite a friend",
                    fontSize = 12.sp,
                    color = Color(0xFFE0F2FE)
                )
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.5.dp
                )
            }
        }
    }
}

@Composable
private fun JoinRoomCard(
    isLoading: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .shadow(12.dp, shape, spotColor = NeonPurple)
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF7C3AED), Color(0xFF6D28D9))
                )
            )
            .clickable(enabled = !isLoading, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("join_room_card"),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Login,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "JOIN ROOM",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = "Enter a code shared by your friend to play",
                    fontSize = 12.sp,
                    color = Color(0xFFEDE9FE)
                )
            }
        }
    }
}

@Composable
private fun QuickMatchCard(
    isLoading: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .shadow(12.dp, shape, spotColor = NeonEmerald)
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF059669), Color(0xFF10B981))
                )
            )
            .clickable(enabled = !isLoading, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("quick_match_card"),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "QUICK MATCH",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = "Find a random online opponent instantly",
                    fontSize = 12.sp,
                    color = Color(0xFFD1FAE5)
                )
            }
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.5.dp
                )
            }
        }
    }
}

