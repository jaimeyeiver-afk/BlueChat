@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.zeni.ia.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeni.ia.ChatViewModel
import com.zeni.ia.ModelState
import com.zeni.ia.R
import kotlinx.coroutines.launch

private val appBackground = Brush.verticalGradient(listOf(Color(0xFF12062B), Color.Black, Color.Black))

@Composable
fun ZeniApp(vm: ChatViewModel) {
    Box(
        Modifier
            .fillMaxSize()
            .background(appBackground)
            .systemBarsPadding()
            .imePadding()
    ) {
        // Sin esto el texto salía negro sobre fondo negro (título "Zeni IA" invisible).
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
            when {
                !vm.serverConfigured || !vm.firebaseConfigured -> SetupScreen(vm)
                !vm.signedIn -> LoginScreen(vm)
                else -> ChatRoot(vm)
            }
        }
    }
}

// ---------------------------------------------------------------- Setup / Login

/** Se muestra solo si esta APK se compiló sin servidor o sin Firebase. El usuario no escribe nada aquí. */
@Composable
fun SetupScreen(vm: ChatViewModel) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Logo(140.dp)
        Spacer(Modifier.height(16.dp))
        Text("Zeni IA", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text("Tu asistente funciona por internet.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(28.dp))
        Surface(color = Panel, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Cloud, null, tint = VioletLight, modifier = Modifier.size(34.dp))
                Spacer(Modifier.height(10.dp))
                Text("Zeni todavía no está conectada", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                Spacer(Modifier.height(6.dp))
                val missing = buildList {
                    if (!vm.serverConfigured) add("el servidor de Zeni")
                    if (!vm.firebaseConfigured) add("el inicio de sesión con Google (Firebase)")
                }.joinToString(" y ")
                Text(
                    "A esta versión de la app le falta configurar $missing. Pide una versión actualizada.",
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun LoginScreen(vm: ChatViewModel) {
    val activity = LocalContext.current.findActivity()
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Logo(140.dp)
        Spacer(Modifier.height(16.dp))
        Text("Zeni IA", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text("Inicia sesión para conversar con Zeni IA.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = { activity?.let { vm.signIn(it) } },
            enabled = !vm.signingIn && activity != null,
            colors = ButtonDefaults.buttonColors(containerColor = Violet, contentColor = Color.White),
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (vm.signingIn) {
                CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.AccountCircle, null)
                Spacer(Modifier.width(10.dp))
                Text("Continuar con Google", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        vm.error?.let { message ->
            Spacer(Modifier.height(16.dp))
            Text(message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, fontSize = 13.sp)
        }
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

// ---------------------------------------------------------------- Chat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoot(vm: ChatViewModel) {
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var menu by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = Panel, modifier = Modifier.width(300.dp)) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Logo(36.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Zeni IA", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
                NavigationDrawerItem(
                    label = { Text("Nuevo chat") },
                    icon = { Icon(Icons.Default.Add, null) },
                    selected = false,
                    onClick = { vm.newChat(); scope.launch { drawer.close() } },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Bubble)
                LazyColumn(Modifier.weight(1f)) {
                    items(vm.conversations, key = { it.id }) { c ->
                        NavigationDrawerItem(
                            label = { Text(c.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            selected = c.id == vm.currentId,
                            onClick = { vm.openChat(c.id); scope.launch { drawer.close() } },
                            badge = {
                                IconButton(onClick = { vm.deleteChat(c.id) }) {
                                    Icon(Icons.Default.Delete, "Borrar", Modifier.size(18.dp))
                                }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }
        }
    ) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Zeni IA", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawer.open() } }) {
                        Icon(Icons.Default.Menu, "Menú")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.newChat() }) { Icon(Icons.Default.Edit, "Nuevo chat") }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Más") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            vm.userLabel?.let { label ->
                                DropdownMenuItem(text = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }, enabled = false, onClick = {})
                            }
                            DropdownMenuItem(
                                text = { Text("Cerrar sesión") },
                                onClick = { menu = false; vm.signOut() }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
            MessagesList(vm, Modifier.weight(1f))
            InputBar(vm)
        }
    }
}

@Composable
fun MessagesList(vm: ChatViewModel, modifier: Modifier) {
    val state = rememberLazyListState()
    val size = vm.messages.size
    val lastLen = vm.messages.lastOrNull()?.text?.length ?: 0
    LaunchedEffect(size, lastLen) {
        if (size > 0) state.scrollToItem(size)
    }

    if (vm.messages.isEmpty()) {
        Column(
            modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Logo(110.dp)
            Spacer(Modifier.height(14.dp))
            Text("¿En qué te ayudo hoy?", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Conectada a Zeni online",
                color = VioletLight,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(20.dp))
            listOf(
                "Explícame algo fácil de entender",
                "Ayúdame a escribir un mensaje",
                "Dame ideas para un proyecto"
            ).forEach { s ->
                Surface(
                    onClick = { vm.send(s) },
                    shape = RoundedCornerShape(16.dp),
                    color = Bubble,
                    modifier = Modifier.padding(vertical = 4.dp).border(1.dp, VioletDeep, RoundedCornerShape(16.dp))
                ) {
                    Text(s, Modifier.padding(horizontal = 16.dp, vertical = 12.dp), fontSize = 14.sp)
                }
            }
        }
        return
    }

    LazyColumn(
        modifier.fillMaxWidth(),
        state = state,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(vm.messages.size) { i ->
            val m = vm.messages[i]
            if (m.role == "user") UserBubble(m.text) else AssistantBubble(m.text)
        }
        item {
            if (vm.generating && (vm.messages.lastOrNull()?.role == "user")) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Logo(28.dp)
                    Spacer(Modifier.width(10.dp))
                    CircularProgressIndicator(Modifier.size(18.dp), color = Violet, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Pensando…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun UserBubble(text: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Box(
            Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp))
                .background(Brush.linearGradient(listOf(Violet, Color(0xFF9D5CFF))))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            SelectionContainer { Text(text, color = Color.White, fontSize = 16.sp) }
        }
    }
}

@Composable
fun AssistantBubble(text: String) {
    Row(Modifier.fillMaxWidth()) {
        Logo(28.dp)
        Spacer(Modifier.width(10.dp))
        SelectionContainer(Modifier.weight(1f)) {
            Text(text, fontSize = 16.sp, lineHeight = 23.sp)
        }
    }
}

@Composable
fun InputBar(vm: ChatViewModel) {
    var text by remember { mutableStateOf("") }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text("Escribe a Zeni IA…") },
            maxLines = 5,
            shape = RoundedCornerShape(26.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Violet,
                unfocusedBorderColor = VioletDeep,
                focusedContainerColor = Panel,
                unfocusedContainerColor = Panel
            )
        )
        Spacer(Modifier.width(8.dp))
        val enabled = text.isNotBlank() && !vm.generating
        Box(
            Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(if (enabled) Violet else VioletDeep)
                .clickable(enabled = enabled) {
                    vm.send(text)
                    text = ""
                },
            contentAlignment = Alignment.Center
        ) {
            if (vm.generating) {
                CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.ArrowUpward, "Enviar", tint = Color.White)
            }
        }
    }
}

@Composable
fun Logo(size: androidx.compose.ui.unit.Dp) {
    Image(
        painter = painterResource(R.drawable.zeni_logo),
        contentDescription = "Zeni IA",
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(size).clip(RoundedCornerShape(size / 4))
    )
}
