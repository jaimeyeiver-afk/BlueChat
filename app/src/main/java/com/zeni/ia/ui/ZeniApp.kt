@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.zeni.ia.ui

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
            if (vm.canChat && !vm.showSetup) ChatRoot(vm) else SetupScreen(vm)
        }
    }
}

// ---------------------------------------------------------------- Setup

@Composable
fun SetupScreen(vm: ChatViewModel) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) vm.importModel(uri)
    }
    var url by remember { mutableStateOf("") }
    var serverUrl by remember { mutableStateOf(vm.backendUrl) }
    val busy = vm.modelState == ModelState.Busy || vm.modelState == ModelState.Loading

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Logo(140.dp)
        Spacer(Modifier.height(16.dp))
        Text("Zeni IA", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Elige cómo quieres usar Zeni: con IA online o con un modelo guardado en tu teléfono.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(28.dp))

        if (busy) {
            Text(vm.status, color = VioletLight)
            Spacer(Modifier.height(12.dp))
            if (vm.modelState == ModelState.Busy && vm.progress > 0f) {
                LinearProgressIndicator(
                    progress = { vm.progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = Violet
                )
                Text("${(vm.progress * 100).toInt()}%", Modifier.padding(top = 6.dp), fontSize = 12.sp)
            } else {
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = Violet)
            }
        } else {
            Text(
                "Conexión online de Zeni",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Pega aquí la dirección HTTPS del servidor. No pongas una clave de Gemini en la aplicación.",
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Dirección del servidor Zeni") },
                placeholder = { Text("https://tu-servidor.example") },
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { vm.enableOnline(serverUrl) },
                enabled = serverUrl.startsWith("https://"),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Violet)
            ) {
                Text("Conectar con Zeni online")
            }
            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = Bubble)
            Spacer(Modifier.height(16.dp))
            Text(
                "O usa un modelo local sin internet",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Necesitas un modelo de IA en formato .task. Se guarda en el teléfono.",
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { picker.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Violet)
            ) {
                Icon(Icons.Default.FolderOpen, null)
                Spacer(Modifier.width(8.dp))
                Text("Elegir modelo del teléfono")
            }
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("o pega un enlace de descarga") },
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { vm.downloadModel(url) },
                enabled = url.startsWith("http"),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Download, null)
                Spacer(Modifier.width(8.dp))
                Text("Descargar una sola vez")
            }
        }

        vm.error?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }
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
                            DropdownMenuItem(
                                text = { Text("Configurar Zeni online") },
                                onClick = { menu = false; vm.openSetup() }
                            )
                            DropdownMenuItem(
                                text = { Text("Cambiar modelo local") },
                                onClick = { menu = false; vm.removeModel() }
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
                if (vm.onlineMode) "Conectada a Zeni online" else "Sin internet · modelo local",
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
