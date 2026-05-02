package com.griffith.chatbot.utils

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.griffith.chatbot.R
import kotlin.math.cos
import kotlin.math.sin

/**
 * An enhanced animated scaffold with improved UI, larger profile dropdown, and sign-out functionality
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimatedScaffold(
    title: String,
    modifier: Modifier = Modifier,
    appLogo: Painter? = null,
    profileImage: Painter? = null,
    userName: String = "",
    userEmail: String = "",
    onSignOut: (() -> Unit)? = null, // New sign-out callback
    content: @Composable (PaddingValues) -> Unit
) {
    /* ------------------------------------------------------------------ */
    /* Background & floating-shape animations                            */
    /* ------------------------------------------------------------------ */
    val infinite = rememberInfiniteTransition(label = "bg_anim")

    val angle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(30_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate"
    )

    val scale by infinite.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            tween(8_000, easing = EaseInOutSine),
             RepeatMode.Reverse
        ),
        label = "scale"
    )

    /* -------------------------------------------------------------- */
    /* Profile drop-down state                                       */
    /* -------------------------------------------------------------- */
    var menuExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        /* ----------------- Enhanced radial-gradient backdrop ------------------- */
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.05f),
                            MaterialTheme.colorScheme.surface
                        ),
                        radius = 1400f * scale
                    )
                )
        )

        /* ----------------- Floating shapes --------------------------- */
        EnhancedFloatingShapes(angle)

        /* ----------------- Main Scaffold ------------------------------ */
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        AppLogo(appLogo)
                    },
                    title = {
                        Text(
                            text = title,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF00BFFF)
                        )
                    },
                    actions = {
                        Box {
                            EnhancedProfileAvatar(
                                image = profileImage,
                                onClick = { menuExpanded = !menuExpanded },
                                fallbackInitial = userName.firstOrNull()?.uppercase() ?: "U"
                            )

                            // Enhanced Profile Dropdown Menu
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                                properties = PopupProperties(
                                    focusable = true,
                                    dismissOnBackPress = true,
                                    dismissOnClickOutside = true
                                ),
                                modifier = Modifier
                                    .width(280.dp) // Increased width
                                    .background(
                                        MaterialTheme.colorScheme.surface,
                                        RoundedCornerShape(16.dp)
                                    )
                            ) {
                                // Profile Header Section
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                                                )
                                            )
                                        )
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Large Profile Avatar
                                    if (profileImage != null) {
                                        Image(
                                            painter = profileImage,
                                            contentDescription = "User avatar",
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.radialGradient(
                                                        colors = listOf(
                                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                                        )
                                                    )
                                                )
                                        ) {
                                            Text(
                                                text = userName.firstOrNull()?.uppercase() ?: "U",
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // User Name
                                    if (userName.isNotBlank()) {
                                        Text(
                                            text = userName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // User Email
                                    if (userEmail.isNotBlank()) {
                                        Text(
                                            text = userEmail,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )

                                // Menu Items
                                Spacer(modifier = Modifier.height(8.dp))

                                // Sign Out Item (replacing Profile item)
                                if (onSignOut != null) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                menuExpanded = false
                                                onSignOut()
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Outlined.ExitToApp,
                                            contentDescription = "Sign out",
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Sign Out",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            content = content
        )
    }
}

/* --------------------------------------------------------------------- */
/* Enhanced circular app logo                                           */
/* --------------------------------------------------------------------- */
@Composable
private fun AppLogo(logo: Painter?) {
    Box(
        modifier = Modifier
            .padding(end = 0.dp)

            .size(98.dp)



    ) {
        if (logo != null) {
            Image(
                painter = logo,
                contentDescription = "App logo",
                modifier = Modifier
                    .size(90.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "App logo",
                modifier = Modifier
                    .size(108.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

/* --------------------------------------------------------------------- */
/* Enhanced circular profile avatar                                     */
/* --------------------------------------------------------------------- */
@Composable
private fun EnhancedProfileAvatar(
    image: Painter?,
    onClick: () -> Unit,
    fallbackInitial: String
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .padding(end = 8.dp)
            .size(48.dp)
    ) {
        if (image != null) {
            Image(
                painter = image,
                contentDescription = "User avatar",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            )
                        )
                    )
            ) {
                Text(
                    text = fallbackInitial,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/* --------------------------------------------------------------------- */
/* Enhanced floating decorative shapes                                  */
/* --------------------------------------------------------------------- */
@Composable
private fun EnhancedFloatingShapes(angleDeg: Float) {
    val rad = Math.toRadians(angleDeg.toDouble()).toFloat()

    // More varied and elegant floating shapes
    FloatingDot(
        x = 50 + 30 * cos(rad * 1.2f),
        y = 140 + 20 * sin(rad * 1.8f),
        size = 60f,
        rotation = angleDeg * 0.3f,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    )

    FloatingDot(
        x = 300 + 40 * cos(rad * 1.6f),
        y = 200 + 30 * sin(rad * 1.3f),
        size = 45f,
        rotation = -angleDeg * 0.2f,
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f)
    )

    FloatingSquare(
        x = 80 + 25 * cos(rad * 2.1f),
        y = 480 + 35 * sin(rad * 1.7f),
        size = 35f,
        rotation = angleDeg * 0.4f,
        corner = 8,
        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.05f)
    )

    FloatingSquare(
        x = 250 + 35 * cos(rad * 1.4f),
        y = 420 + 25 * sin(rad * 2.3f),
        size = 50f,
        rotation = angleDeg * 0.25f,
        corner = 12,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.07f)
    )

    // Additional shapes for more dynamic background
    FloatingDot(
        x = 150 + 20 * cos(rad * 2.5f),
        y = 320 + 15 * sin(rad * 1.9f),
        size = 25f,
        rotation = angleDeg * 0.5f,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.04f)
    )

    FloatingSquare(
        x = 200 + 15 * cos(rad * 1.8f),
        y = 600 + 20 * sin(rad * 2.1f),
        size = 30f,
        rotation = -angleDeg * 0.3f,
        corner = 6,
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f)
    )
}

@Composable
private fun FloatingDot(
    x: Float,
    y: Float,
    size: Float,
    rotation: Float,
    color: Color
) {
    Box(
        Modifier
            .offset(x.dp, y.dp)
            .size(size.dp)
            .rotate(rotation)
            .background(color, shape = CircleShape)
    )
}

@Composable
private fun FloatingSquare(
    x: Float,
    y: Float,
    size: Float,
    rotation: Float,
    corner: Int,
    color: Color
) {
    Box(
        Modifier
            .offset(x.dp, y.dp)
            .size(size.dp)
            .rotate(rotation)
            .background(color, shape = RoundedCornerShape(corner.dp))
    )
}