package com.example.gecospro

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF2196F3),
                    primaryContainer = Color(0xFFBBDEFB),
                    secondary = Color(0xFF03A9F4),
                    tertiary = Color(0xFF4FC3F7),
                    background = Color(0xFFF5F9FF),
                    surface = Color.White,
                    surfaceVariant = Color(0xFFE3F2FD),
                    onPrimary = Color.White,
                    onSecondary = Color.White,
                    onSurface = Color(0xFF1A1A1A),
                    onBackground = Color(0xFF1A1A1A),
                    error = Color(0xFFB00020)
                )
            ) {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "turni"
    ) {
        composable("turni") {
            TurniScreen(navController)
        }
        composable("sedi") {
            SediScreen(navController)
        }
        composable("assegnazioni") {
            AssegnazioniScreen(navController)
        }
        composable("impostazioni") {
            ImpostazioniScreen(navController)
        }
    }
}

@Composable
fun BottomNavBar(navController: NavHostController) {
    val items = listOf(
        Triple("turni", "Turni", Icons.Default.Schedule),
        Triple("sedi", "Sedi", Icons.Default.LocationOn),
        Triple("assegnazioni", "Assegnazioni", Icons.Default.Assignment),
        Triple("impostazioni", "Impostazioni", Icons.Default.Settings)
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        val currentRoute = navController.currentBackStackEntry?.destination?.route

        items.forEach { item ->
            val route = item.first
            val label = item.second
            val icon = item.third

            NavigationBarItem(
                selected = currentRoute == route,
                onClick = {
                    if (currentRoute != route) {
                        navController.navigate(route) {
                            popUpTo("turni") {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label
                    )
                },
                label = {
                    Text(label)
                }
            )
        }
    }
}

@Composable
fun TurniScreen(navController: NavHostController) {
    val ctx = LocalContext.current
    var showAdd by remember { mutableStateOf(false) }
    var versione by remember { mutableStateOf(0) }

    fun riapplica() {
        Scheduler.programmaProssimo(ctx)
        versione++
    }

    Scaffold(
        bottomBar = {
            BottomNavBar(navController)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    showAdd = true
                }
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Aggiungi turno"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "I Miei Turni",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val turni = remember(versione) {
                Store.turni(ctx)
            }

            val sedi = remember(versione) {
                Store.sedi(ctx)
            }

            val assegnazioni = remember(versione) {
                Store.assegnazioni(ctx)
            }

            if (turni.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nessun turno configurato.\nPremi + per iniziare.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        turni.sortedWith(
                            compareBy<Turno> { it.giorno }
                                .thenBy { it.ora }
                                .thenBy { it.minuto }
                        )
                    ) { turno ->
                        val sede = sedi.find {
                            it.id == turno.sedeId
                        }

                        val assegnazione = assegnazioni.find {
                            it.id == turno.assegnazioneId
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${nomeGiorno(turno.giorno)} ${hhmm(turno.ora, turno.minuto)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Switch(
                                        checked = turno.attivo,
                                        onCheckedChange = {
                                            turno.attivo = it
                                            Store.salvaTurni(ctx, turni)
                                            riapplica()
                                        }
                                    )
                                }

                                Text(
                                    text = "${if (turno.tipo == Tipo.INGRESSO) "INGRESSO" else "USCITA"} • ${sede?.nome ?: "Sede non configurata"}",
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                if (assegnazione != null) {
                                    Text(
                                        text = assegnazione.nome,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    IconButton(
                                        onClick = {
                                            turni.remove(turno)
                                            Store.salvaTurni(ctx, turni)
                                            riapplica()
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Elimina",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AggiungiTurnoDialog(
            onDismiss = {
                showAdd = false
            },
            onAdd = { nuovoTurno ->
                val turni = Store.turni(ctx).toMutableList()
                turni.add(nuovoTurno)
                Store.salvaTurni(ctx, turni)
                showAdd = false
                riapplica()
            }
        )
    }
}

@Composable
fun SediScreen(navController: NavHostController) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var showAdd by remember { mutableStateOf(false) }
    var versione by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            BottomNavBar(navController)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    showAdd = true
                }
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Aggiungi sede"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Sedi",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val sedi = remember(versione) {
                Store.sedi(ctx)
            }

            if (sedi.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nessuna sede salvata.\nPremi + per aggiungerne una.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sedi) { sede ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = sede.nome,
                                    style = MaterialTheme.typography.titleMedium
                                )

                                if (sede.indirizzo.isNotBlank()) {
                                    Text(
                                        text = sede.indirizzo,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                Spacer(
                                    modifier = Modifier.height(6.dp)
                                )

                                Text(
                                    text = if (sede.lat == 0.0 && sede.lng == 0.0) {
                                        "⚠ Posizione GPS non impostata"
                                    } else {
                                        "📍 Coordinate memorizzate • raggio ${sede.raggio} m"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (sede.lat == 0.0 && sede.lng == 0.0) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    }
                                )

                                Spacer(
                                    modifier = Modifier.height(12.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                val loc = Geo.posizione(ctx)

                                                if (loc != null) {
                                                    sede.lat = loc.latitude
                                                    sede.lng = loc.longitude
                                                    Store.salvaSedi(ctx, sedi)
                                                    versione++
                                                }
                                            }
                                        },
                                        enabled = ContextCompat.checkSelfPermission(
                                            ctx,
                                            Manifest.permission.ACCESS_FINE_LOCATION
                                        ) == PackageManager.PERMISSION_GRANTED
                                    ) {
                                        Text("Sono qui ora")
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = {
                                                if (sede.raggio > 100) {
                                                    sede.raggio -= 50
                                                    Store.salvaSedi(ctx, sedi)
                                                    versione++
                                                }
                                            }
                                        ) {
                                            Text("−")
                                        }

                                        Text("${sede.raggio} m")

                                        IconButton(
                                            onClick = {
                                                if (sede.raggio < 2000) {
                                                    sede.raggio += 50
                                                    Store.salvaSedi(ctx, sedi)
                                                    versione++
                                                }
                                            }
                                        ) {
                                            Text("+")
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            sedi.remove(sede)
                                            Store.salvaSedi(ctx, sedi)
                                            versione++
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Elimina",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var nome by remember { mutableStateOf("") }
        var indirizzo by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = {
                showAdd = false
            },
            title = {
                Text("Nuova sede")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (nome.isNotBlank()) {
                            val sedi = Store.sedi(ctx).toMutableList()

                            sedi.add(
                                Sede(
                                    nome = nome.trim(),
                                    indirizzo = indirizzo.trim()
                                )
                            )

                            Store.salvaSedi(ctx, sedi)
                            showAdd = false
                            versione++
                        }
                    },
                    enabled = nome.isNotBlank()
                ) {
                    Text("Salva")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAdd = false
                    }
                ) {
                    Text("Annulla")
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = nome,
                        onValueChange = {
                            nome = it
                        },
                        label = {
                            Text("Nome sede")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = indirizzo,
                        onValueChange = {
                            indirizzo = it
                        },
                        label = {
                            Text("Indirizzo")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        )
    }
}

@Composable
fun AssegnazioniScreen(navController: NavHostController) {
    val ctx = LocalContext.current
    var showAdd by remember { mutableStateOf(false) }
    var versione by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            BottomNavBar(navController)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    showAdd = true
                }
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Aggiungi assegnazione"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Assegnazioni Gecos",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val assegnazioni = remember(versione) {
                Store.assegnazioni(ctx)
            }

            if (assegnazioni.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nessuna assegnazione salvata.\nPremi + per aggiungerne una.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(assegnazioni) { assegnazione ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = assegnazione.nome,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )

                                IconButton(
                                    onClick = {
                                        assegnazioni.remove(assegnazione)
                                        Store.salvaAssegnazioni(ctx, assegnazioni)
                                        versione++
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Elimina",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var nome by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = {
                showAdd = false
            },
            title = {
                Text("Nuova assegnazione")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (nome.isNotBlank()) {
                            val assegnazioni = Store.assegnazioni(ctx).toMutableList()

                            assegnazioni.add(
                                Assegnazione(
                                    nome = nome.trim()
                                )
                            )

                            Store.salvaAssegnazioni(ctx, assegnazioni)
                            showAdd = false
                            versione++
                        }
                    },
                    enabled = nome.isNotBlank()
                ) {
                    Text("Salva")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAdd = false
                    }
                ) {
                    Text("Annulla")
                }
            },
            text = {
                OutlinedTextField(
                    value = nome,
                    onValueChange = {
                        nome = it
                    },
                    label = {
                        Text("Nome assegnazione Gecos")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }
}

@Composable
fun ImpostazioniScreen(navController: NavHostController) {
    val ctx = LocalContext.current

    var versione by remember { mutableStateOf(0) }
    var notifOk by remember { mutableStateOf(false) }
    var locOk by remember { mutableStateOf(false) }
    var bgOk by remember { mutableStateOf(false) }
    var alarmOk by remember { mutableStateOf(false) }
    var showApps by remember { mutableStateOf(false) }

    fun aggiorna() {
        notifOk = ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        locOk = ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        bgOk = ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        alarmOk = if (Build.VERSION.SDK_INT >= 31) {
            (ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
                .canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun riapplica() {
        aggiorna()

        if (notifOk && locOk && alarmOk) {
            val occ = Store.occ(ctx)

            if (occ != null && !Store.fatto(ctx, occ.turnoId, occ.dataKey)) {
                Scheduler.riprova(ctx, occ.turnoId, occ.dataKey, occ.start, 0)
            } else {
                Scheduler.programmaProssimo(ctx)
            }
        }

        versione++
    }

    LaunchedEffect(Unit) {
        aggiorna()
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        riapplica()
    }

    val bgLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        riapplica()
    }

    val soundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            @Suppress("DEPRECATION")
            val uri: Uri? = res.data?.getParcelableExtra(
                RingtoneManager.EXTRA_RINGTONE_PICKED_URI
            )

            val imp = Store.impostazioni(ctx)
            imp.suonoUri = uri?.toString()
            Store.salvaImpostazioni(ctx, imp)

            versione++
        }
    }

    val imp = remember(versione) {
        Store.impostazioni(ctx)
    }

    val prossimo = remember(versione) {
        Scheduler.prossimo(ctx)
    }

    Scaffold(
        bottomBar = {
            BottomNavBar(navController)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Impostazioni",
                style = MaterialTheme.typography.headlineMedium
            )

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Stato generale",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Abilita tutti i promemoria",
                            modifier = Modifier.weight(1f)
                        )

                        Switch(
                            checked = imp.master,
                            onCheckedChange = {
                                imp.master = it
                                Store.salvaImpostazioni(ctx, imp)
                                riapplica()
                            }
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Prossimo promemoria",
                        style = MaterialTheme.typography.titleMedium
                    )

                    val p = prossimo

                    if (p == null) {
                        Text("Nessun turno inserito o attivo.")
                    } else {
                        val turno = p.first
                        val cal = p.second

                        Text(
                            text = "${nomeGiorno(cal.get(Calendar.DAY_OF_WEEK))} ore ${hhmm(turno.ora, turno.minuto)}"
                        )
                    }

                    Button(
                        onClick = {
                            val t0 = Store.turni(ctx).firstOrNull()

                            if (t0 != null) {
                                val sede = Store.sedi(ctx).find {
                                    it.id == t0.sedeId
                                }

                                val assegnazione = Store.assegnazioni(ctx).find {
                                    it.id == t0.assegnazioneId
                                }

                                Notifiche.mostra(
                                    ctx,
                                    t0,
                                    sede,
                                    assegnazione,
                                    dataKey(Calendar.getInstance()),
                                    false
                                )
                            } else {
                                val testTurno = Turno(
                                    sedeId = "",
                                    assegnazioneId = "",
                                    tipo = Tipo.INGRESSO,
                                    ora = 12,
                                    minuto = 0,
                                    giorno = Calendar.MONDAY
                                )

                                val testSede = Sede(
                                    nome = "Sede Test",
                                    indirizzo = "Via Roma 1"
                                )

                                val testAss = Assegnazione(
                                    nome = "Servizio Esempio Gecos"
                                )

                                Notifiche.mostra(
                                    ctx,
                                    testTurno,
                                    testSede,
                                    testAss,
                                    dataKey(Calendar.getInstance()),
                                    false
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Invia notifica di prova")
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Autorizzazioni",
                        style = MaterialTheme.typography.titleMedium
                    )

                    PermRow(
                        nome = "Notifiche",
                        ok = notifOk
                    ) {
                        permLauncher.launch(
                            arrayOf(
                                Manifest.permission.POST_NOTIFICATIONS
                            )
                        )
                    }

                    PermRow(
                        nome = "Posizione GPS",
                        ok = locOk
                    ) {
                        permLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }

                    PermRow(
                        nome = "Posizione in background",
                        ok = bgOk,
                        enabled = locOk
                    ) {
                        bgLauncher.launch(
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
                    }

                    PermRow(
                        nome = "Sveglie esatte",
                        ok = alarmOk
                    ) {
                        if (Build.VERSION.SDK_INT >= 31) {
                            try {
                                ctx.startActivity(
                                    Intent(
                                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                        Uri.parse("package:${ctx.packageName}")
                                    )
                                )
                            } catch (_: Exception) {
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Audio e app",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Suono notifica")

                            Text(
                                text = if (imp.suonoUri == null) {
                                    "Predefinito di sistema"
                                } else {
                                    "Personalizzato impostato"
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Button(
                            onClick = {
                                val i = Intent(
                                    RingtoneManager.ACTION_RINGTONE_PICKER
                                )
                                    .putExtra(
                                        RingtoneManager.EXTRA_RINGTONE_TYPE,
                                        RingtoneManager.TYPE_NOTIFICATION
                                    )
                                    .putExtra(
                                        RingtoneManager.EXTRA_RINGTONE_TITLE,
                                        "Scegli suono"
                                    )
                                    .putExtra(
                                        RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                        imp.suonoUri?.let {
                                            Uri.parse(it)
                                        }
                                    )

                                soundLauncher.launch(i)
                            }
                        ) {
                            Text("Cambia")
                        }
                    }

                    HorizontalDivider()

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("App da aprire")

                            val labelApp = imp.appPackage?.let { pkg ->
                                try {
                                    @Suppress("DEPRECATION")
                                    ctx.packageManager.getApplicationLabel(
                                        ctx.packageManager.getApplicationInfo(pkg, 0)
                                    ).toString()
                                } catch (_: Exception) {
                                    pkg
                                }
                            } ?: "Non impostata"

                            Text(
                                text = labelApp,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Button(
                            onClick = {
                                showApps = true
                            }
                        ) {
                            Text("Scegli")
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Ripetizione promemoria",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (imp.ripetiOgniMin > 1) {
                                    imp.ripetiOgniMin--
                                    Store.salvaImpostazioni(ctx, imp)
                                    versione++
                                }
                            }
                        ) {
                            Text("−")
                        }

                        Text(
                            text = "Ogni ${imp.ripetiOgniMin} minuti",
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = {
                                if (imp.ripetiOgniMin < 60) {
                                    imp.ripetiOgniMin++
                                    Store.salvaImpostazioni(ctx, imp)
                                    versione++
                                }
                            }
                        ) {
                            Text("+")
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (imp.ripetiMaxMin > 5) {
                                    imp.ripetiMaxMin -= 5
                                    Store.salvaImpostazioni(ctx, imp)
                                    versione++
                                }
                            }
                        ) {
                            Text("−")
                        }

                        Text(
                            text = "Per massimo ${imp.ripetiMaxMin} minuti",
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = {
                                if (imp.ripetiMaxMin < 240) {
                                    imp.ripetiMaxMin += 5
                                    Store.salvaImpostazioni(ctx, imp)
                                    versione++
                                }
                            }
                        ) {
                            Text("+")
                        }
                    }
                }
            }
        }

        if (showApps) {
            val apps = remember {
                elencoApp(ctx)
            }

            AlertDialog(
                onDismissRequest = {
                    showApps = false
                },
                title = {
                    Text("Scegli Gecos Plus")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showApps = false
                        }
                    ) {
                        Text("Chiudi")
                    }
                },
                text = {
                    Box(
                        modifier = Modifier.height(320.dp)
                    ) {
                        LazyColumn {
                            items(apps) { app ->
                                TextButton(
                                    onClick = {
                                        val imp2 = Store.impostazioni(ctx)
                                        imp2.appPackage = app.first
                                        Store.salvaImpostazioni(ctx, imp2)
                                        showApps = false
                                        versione++
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(app.second)
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun AggiungiTurnoDialog(
    onDismiss: () -> Unit,
    onAdd: (Turno) -> Unit
) {
    val ctx = LocalContext.current

    val sedi = Store.sedi(ctx)
    val assegnazioni = Store.assegnazioni(ctx)

    var selectedSede by remember {
        mutableStateOf(sedi.firstOrNull()?.id ?: "")
    }

    var selectedAssegnazione by remember {
        mutableStateOf(assegnazioni.firstOrNull()?.id ?: "")
    }

    var selectedGiorno by remember {
        mutableStateOf(Calendar.MONDAY)
    }

    var selectedTipo by remember {
        mutableStateOf(Tipo.INGRESSO)
    }

    var ora by remember {
        mutableStateOf(8)
    }

    var minuto by remember {
        mutableStateOf(45)
    }

    var showTimePicker by remember {
        mutableStateOf(false)
    }

    if (showTimePicker) {
        TimePickerDialog(
            ctx,
            { _, h, m ->
                ora = h
                minuto = m
            },
            ora,
            minuto,
            true
        ).show()

        showTimePicker = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Aggiungi turno")
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (
                        selectedSede.isNotBlank() &&
                        selectedAssegnazione.isNotBlank()
                    ) {
                        onAdd(
                            Turno(
                                sedeId = selectedSede,
                                assegnazioneId = selectedAssegnazione,
                                tipo = selectedTipo,
                                ora = ora,
                                minuto = minuto,
                                giorno = selectedGiorno
                            )
                        )
                    }
                },
                enabled = selectedSede.isNotBlank() &&
                    selectedAssegnazione.isNotBlank()
            ) {
                Text("Crea")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Annulla")
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (sedi.isEmpty()) {
                    Text(
                        text = "⚠ Crea prima almeno una sede.",
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    DropdownSelector(
                        label = "Sede",
                        selected = sedi.find {
                            it.id == selectedSede
                        }?.nome ?: "Scegli sede",
                        items = sedi.map {
                            it.nome to it.id
                        },
                        onSelected = {
                            selectedSede = it as String
                        }
                    )
                }

                if (assegnazioni.isEmpty()) {
                    Text(
                        text = "⚠ Crea prima almeno un'assegnazione.",
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    DropdownSelector(
                        label = "Assegnazione",
                        selected = assegnazioni.find {
                            it.id == selectedAssegnazione
                        }?.nome ?: "Scegli assegnazione",
                        items = assegnazioni.map {
                            it.nome to it.id
                        },
                        onSelected = {
                            selectedAssegnazione = it as String
                        }
                    )
                }

                DropdownSelector(
                    label = "Giorno",
                    selected = nomeGiorno(selectedGiorno),
                    items = listOf(
                        "Lunedì" to Calendar.MONDAY,
                        "Martedì" to Calendar.TUESDAY,
                        "Mercoledì" to Calendar.WEDNESDAY,
                        "Giovedì" to Calendar.THURSDAY,
                        "Venerdì" to Calendar.FRIDAY,
                        "Sabato" to Calendar.SATURDAY,
                        "Domenica" to Calendar.SUNDAY
                    ),
                    onSelected = {
                        selectedGiorno = it as Int
                    }
                )

                DropdownSelector(
                    label = "Tipo",
                    selected = if (selectedTipo == Tipo.INGRESSO) {
                        "Ingresso"
                    } else {
                        "Uscita"
                    },
                    items = listOf(
                        "Ingresso" to Tipo.INGRESSO,
                        "Uscita" to Tipo.USCITA
                    ),
                    onSelected = {
                        selectedTipo = it as Tipo
                    }
                )

                Button(
                    onClick = {
                        showTimePicker = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Orario: ${hhmm(ora, minuto)}")
                }
            }
        }
    )
}

@Composable
fun DropdownSelector(
    label: String,
    selected: String,
    items: List<Pair<String, Any>>,
    onSelected: (Any) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedButton(
            onClick = {
                expanded = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("$label: $selected")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(item.first)
                    },
                    onClick = {
                        onSelected(item.second)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun PermRow(
    nome: String,
    ok: Boolean,
    enabled: Boolean = true,
    onAsk: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = if (ok) {
                Icons.Default.Check
            } else {
                Icons.Default.Warning
            },
            contentDescription = if (ok) {
                "OK"
            } else {
                "Da concedere"
            },
            tint = if (ok) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            }
        )

        Text(
            text = nome,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        )

        if (!ok) {
            TextButton(
                enabled = enabled,
                onClick = onAsk
            ) {
                Text("Concedi")
            }
        }
    }
}

fun elencoApp(c: Context): List<Pair<String, String>> {
    val intent = Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_LAUNCHER)

    return c.packageManager
        .queryIntentActivities(intent, 0)
        .mapNotNull { resolveInfo ->
            val packageName = resolveInfo.activityInfo?.packageName
                ?: return@mapNotNull null

            val label = try {
                resolveInfo.loadLabel(c.packageManager).toString()
            } catch (_: Exception) {
                packageName
            }

            packageName to label
        }
        .distinctBy {
            it.first
        }
        .filter {
            it.first != c.packageName
        }
        .sortedBy {
            it.second.lowercase()
        }
}
