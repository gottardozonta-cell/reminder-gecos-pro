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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController // <-- Importazione corretta aggiunta
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

class MainActivity : ComponentActivity() {

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        setContent { MaterialTheme { AppContent() } }
    }
}

@Composable
fun AppContent() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "turni") {
        composable("turni") { TurniScreen(navController) }
        composable("sedi") { SediScreen(navController) }
        composable("assegnazioni") { AssegnazioniScreen(navController) }
        composable("impostazioni") { ImpostazioniScreen(navController) }
    }
}

@Composable
fun BottomNavBar(navController: NavHostController) {
    val items = listOf(
        Triple("turni", "I Miei Turni", Icons.Default.Schedule),
        Triple("sedi", "Sedi", Icons.Default.LocationOn),
        Triple("assegnazioni", "Assegnazioni", Icons.Default.Assignment),
        Triple("impostazioni", "Impostazioni", Icons.Default.Settings)
    )
    NavigationBar {
        items.forEach { (route, label, icon) ->
            NavigationBarItem(
                selected = navController.currentBackStackEntry?.destination?.route == route,
                onClick = { navController.navigate(route) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
fun TurniScreen(navController: NavHostController) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var showAdd by remember { mutableStateOf(false) }
    var versione by remember { mutableStateOf(0) }

    fun riapplica() {
        Scheduler.programmaProssimo(ctx)
        versione++
    }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f).padding(16.dp)) {
            val turni = remember(versione) { Store.turni(ctx) }
            val sedi = remember(versione) { Store.sedi(ctx) }
            val assegnazioni = remember(versione) { Store.assegnazioni(ctx) }

            if (turni.isEmpty()) {
                item {
                    Text(
                        "Nessun turno configurato. Aggiungine uno!",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 32.dp)
                    )
                }
            } else {
                items(turni.sortedWith(compareBy({ it.giorno }, { it.ora }, { it.minuto }))) { t ->
                    val sede = sedi.find { it.id == t.sedeId }
                    val assegnazione = assegnazioni.find { it.id == t.assegnazioneId }
                    Card(
                        onClick = { /* TODO: Edit turno */ },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "${nomeGiorno(t.giorno)} ${hhmm(t.ora, t.minuto)}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "${if (t.tipo == Tipo.INGRESSO) "INGRESSO" else "USCITA"} • ${sede?.nome ?: "Sede non trovata"}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (assegnazione != null) {
                                Text(
                                    assegnazione.nome,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Switch(
                                    checked = t.attivo,
                                    onCheckedChange = {
                                        t.attivo = it
                                        Store.salvaTurni(ctx, turni)
                                        riapplica()
                                    }
                                )
                                IconButton(onClick = {
                                    turni.remove(t)
                                    Store.salvaTurni(ctx, turni)
                                    riapplica()
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Elimina")
                                }
                            }
                        }
                    }
                }
            }
        }
        BottomNavBar(navController)
        FloatingActionButton(
            onClick = { showAdd = true },
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.BottomEnd)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Aggiungi turno")
        }
    }

    if (showAdd) {
        AggiungiTurnoDialog(
            onDismiss = { showAdd = false },
            onAdd = {
                val turni = Store.turni(ctx).toMutableList()
                turni.add(it)
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

    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f).padding(16.dp)) {
            val sedi = remember(versione) { Store.sedi(ctx) }
            if (sedi.isEmpty()) {
                item {
                    Text(
                        "Nessuna sede configurata. Aggiungine una!",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 32.dp)
                    )
                }
            } else {
                items(sedi) { s ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(s.nome, style = MaterialTheme.typography.titleMedium)
                            Text(s.indirizzo, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                if (s.lat == 0.0) "⚠ GPS non impostato"
                                else "📍 ${"%.2f".format(s.lat)}, ${"%.2f".format(s.lng)} (Raggio: ${s.raggio}m)",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            val loc = Geo.posizione(ctx)
                                            if (loc != null) {
                                                s.lat = loc.latitude
                                                s.lng = loc.longitude
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
                                    Text("Imposta GPS qui")
                                }
                                IconButton(onClick = {
                                    sedi.remove(s)
                                    Store.salvaSedi(ctx, sedi)
                                    versione++
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Elimina")
                                }
                            }
                        }
                    }
                }
            }
        }
        BottomNavBar(navController)
        FloatingActionButton(
            onClick = { showAdd = true },
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.BottomEnd)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Aggiungi sede")
        }
    }

    if (showAdd) {
        var nome by remember { mutableStateOf("") }
        var indirizzo by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Nuova Sede") },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (nome.isNotBlank()) {
                            val sedi = Store.sedi(ctx).toMutableList()
                            sedi.add(Sede(nome = nome, indirizzo = indirizzo))
                            Store.salvaSedi(ctx, sedi)
                            showAdd = false
                            versione++
                        }
                    },
                    enabled = nome.isNotBlank()
                ) { Text("Aggiungi") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Annulla") } },
            text = {
                Column {
                    OutlinedTextField(
                        value = nome,
                        onValueChange = { nome = it },
                        label = { Text("Nome sede") },
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = indirizzo,
                        onValueChange = { indirizzo = it },
                        label = { Text("Indirizzo") },
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

    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f).padding(16.dp)) {
            val assegnazioni = remember(versione) { Store.assegnazioni(ctx) }
            if (assegnazioni.isEmpty()) {
                item {
                    Text(
                        "Nessuna assegnazione configurata. Aggiungine una!",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 32.dp)
                    )
                }
            } else {
                items(assegnazioni) { a ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(a.nome, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = {
                                assegnazioni.remove(a)
                                Store.salvaAssegnazioni(ctx, assegnazioni)
                                versione++
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Elimina")
                            }
                        }
                    }
                }
            }
        }
        BottomNavBar(navController)
        FloatingActionButton(
            onClick = { showAdd = true },
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.BottomEnd)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Aggiungi assegnazione")
        }
    }

    if (showAdd) {
        var nome by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Nuova Assegnazione") },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (nome.isNotBlank()) {
                            val assegnazioni = Store.assegnazioni(ctx).toMutableList()
                            assegnazioni.add(Assegnazione(nome = nome))
                            Store.salvaAssegnazioni(ctx, assegnazioni)
                            showAdd = false
                            versione++
                        }
                    },
                    enabled = nome.isNotBlank()
                ) { Text("Aggiungi") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Annulla") } },
            text = {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome assegnazione (copia esatto da Gecos)") },
                    singleLine = true
                )
            }
        )
    }
}

@Composable
fun ImpostazioniScreen(navController: NavHostController) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var versione by remember { mutableStateOf(0) }
    var notifOk by remember { mutableStateOf(false) }
    var locOk by remember { mutableStateOf(false) }
    var bgOk by remember { mutableStateOf(false) }
    var alarmOk by remember { mutableStateOf(false) }
    var showApps by remember { mutableStateOf(false) }

    fun aggiorna() {
        notifOk = ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        locOk = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        bgOk = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        alarmOk = if (Build.VERSION.SDK_INT >= 31)
            (ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms() else true
    }

    fun riapplica() {
        aggiorna()
        if (notifOk && locOk && alarmOk) {
            val occ = Store.occ(ctx)
            if (occ != null && !Store.fatto(ctx, occ.turnoId, occ.dataKey))
                Scheduler.riprova(ctx, occ.turnoId, occ.dataKey, occ.start, 0)
            else Scheduler.programmaProssimo(ctx)
        }
        versione++
    }

    LaunchedEffect(Unit) { aggiorna() }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { riapplica() }
    val bgLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { riapplica() }
    val soundLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            @Suppress("DEPRECATION")
            val uri: Uri? = res.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            val imp = Store.impostazioni(ctx)
            imp.suonoUri = uri?.toString()
            Store.salvaImpostazioni(ctx, imp)
            versione++
        }
    }

    val imp = remember(versione) { Store.impostazioni(ctx) }
    val prossimo = remember(versione) { Scheduler.prossimo(ctx) }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f).padding(16.dp)) {
            item {
                Card {
                    Column(Modifier.padding(16.dp)) {
                        Text("Promemoria attivi", style = MaterialTheme.typography.titleMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Abilita notifiche", Modifier.weight(1f))
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
            }
            item {
                Card {
                    Column(Modifier.padding(16.dp)) {
                        Text("Prossimo turno", style = MaterialTheme.typography.titleMedium)
                        val p = prossimo
                        if (p == null) Text("Nessun turno programmato")
                        else {
                            val (t, cal, _) = p
                            Text("${nomeGiorno(cal.get(Calendar.DAY_OF_WEEK))} ${hhmm(t.ora, t.minuto)}")
                        }
                        OutlinedButton(onClick = {
                            val t0 = Store.turni(ctx).firstOrNull()
                            if (t0 != null) {
                                val sede = Store.sedi(ctx).find { it.id == t0.sedeId }
                                val assegnazione = Store.assegnazioni(ctx).find { it.id == t0.assegnazioneId }
                                Notifiche.mostra(ctx, t0, sede, assegnazione, dataKey(Calendar.getInstance()), false)
                            }
                        }) { Text("Prova notifica") }
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.padding(16.dp)) {
                        Text("Permessi", style = MaterialTheme.typography.titleMedium)
                        PermRow("Notifiche", notifOk) {
                            permLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                        }
                        PermRow("Posizione (app attiva)", locOk) {
                            permLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                        }
                        PermRow("Posizione in background", bgOk, enabled = locOk) {
                            bgLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        }
                        PermRow("Sveglie esatte", alarmOk) {
                            try {
                                ctx.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}")))
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Suono notifica", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                val i = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                                    .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                                    .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Suono promemoria")
                                    .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, imp.suonoUri?.let { Uri.parse(it) })
                                soundLauncher.launch(i)
                            }) { Text("Scegli suono") }
                            if (imp.suonoUri != null) {
                                OutlinedButton(onClick = {
                                    try { RingtoneManager.getRingtone(ctx, Uri.parse(imp.suonoUri))?.play() } catch (_: Exception) {}
                                }) { Text("Prova") }
                                OutlinedButton(onClick = {
                                    val i2 = Store.impostazioni(ctx)
                                    i2.suonoUri = null
                                    Store.salvaImpostazioni(ctx, i2)
                                    versione++
                                }) { Text("Default") }
                            }
                        }
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.padding(16.dp)) {
                        Text("App da aprire", style = MaterialTheme.typography.titleMedium)
                        val lbl = imp.appPackage?.let { pkg ->
                            try {
                                @Suppress("DEPRECATION")
                                ctx.packageManager.getApplicationLabel(ctx.packageManager.getApplicationInfo(pkg, 0)).toString()
                            } catch (_: Exception) { pkg }
                        } ?: "Non impostata"
                        Text(lbl, style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick = { showApps = true }) { Text("Scegli app") }
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Ripetizione notifica", style = MaterialTheme.typography.titleMedium)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { imp.ripetiOgniMin = (imp.ripetiOgniMin - 1).coerceAtLeast(1); Store.salvaImpostazioni(ctx, imp); versione++ },
                                enabled = imp.ripetiOgniMin > 1
                            ) { Text("−") }
                            Text("Ogni ${imp.ripetiOgniMin} min", Modifier.weight(1f))
                            OutlinedButton(onClick = { imp.ripetiOgniMin = (imp.ripetiOgniMin + 1).coerceAtMost(60); Store.salvaImpostazioni(ctx, imp); versione++ }) { Text("+") }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { imp.ripetiMaxMin = (imp.ripetiMaxMin - 5).coerceAtLeast(5); Store.salvaImpostazioni(ctx, imp); versione++ },
                                enabled = imp.ripetiMaxMin > 5
                            ) { Text("−") }
                            Text("Max ${imp.ripetiMaxMin} min", Modifier.weight(1f))
                            OutlinedButton(onClick = { imp.ripetiMaxMin = (imp.ripetiMaxMin + 5).coerceAtMost(240); Store.salvaImpostazioni(ctx, imp); versione++ }) { Text("+") }
                        }
                    }
                }
            }
        }
        BottomNavBar(navController)

        if (showApps) {
            AlertDialog(
                onDismissRequest = { showApps = false },
                title = { Text("Scegli Gecos Plus") },
                confirmButton = { TextButton(onClick = { showApps = false }) { Text("Chiudi") } },
                text = {
                    LazyColumn(Modifier.verticalScroll(rememberScrollState())) {
                        items(elencoApp(ctx)) { (pkg, label) ->
                            TextButton(onClick = {
                                val i3 = Store.impostazioni(ctx)
                                i3.appPackage = pkg
                                Store.salvaImpostazioni(ctx, i3)
                                showApps = false
                                versione++
                            }) { Text(label) }
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
    var selectedSede by remember { mutableStateOf(sedi.firstOrNull()?.id ?: "") }
    var selectedAssegnazione by remember { mutableStateOf(assegnazioni.firstOrNull()?.id ?: "") }
    var selectedGiorno by remember { mutableStateOf(Calendar.MONDAY) }
    var selectedTipo by remember { mutableStateOf(Tipo.INGRESSO) }
    var ora by remember { mutableStateOf(8) }
    var minuto by remember { mutableStateOf(0) }
    var showTimePicker by remember { mutableStateOf(false) }

    if (showTimePicker) {
        TimePickerDialog(
            ctx,
            { _, h, m -> ora = h; minuto = m },
            ora,
            minuto,
            true
        ).show()
        showTimePicker = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuovo Turno") },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedSede.isNotBlank() && selectedAssegnazione.isNotBlank()) {
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
                enabled = selectedSede.isNotBlank() && selectedAssegnazione.isNotBlank()
            ) { Text("Aggiungi") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DropdownMenuItem(
                    label = "Sede",
                    selected = sedi.find { it.id == selectedSede }?.nome ?: "Seleziona",
                    items = sedi.map { it.nome to it.id },
                    onSelected = { selectedSede = it }
                )
                DropdownMenuItem(
                    label = "Assegnazione",
                    selected = assegnazioni.find { it.id == selectedAssegnazione }?.nome ?: "Seleziona",
                    items = assegnazioni.map { it.nome to it.id },
                    onSelected = { selectedAssegnazione = it }
                )
                DropdownMenuItem(
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
                    onSelected = { selectedGiorno = it }
                )
                DropdownMenuItem(
                    label = "Tipo",
                    selected = if (selectedTipo == Tipo.INGRESSO) "Ingresso" else "Uscita",
                    items = listOf("Ingresso" to Tipo.INGRESSO, "Uscita" to Tipo.USCITA),
                    onSelected = { selectedTipo = it }
                )
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ora: ${hhmm(ora, minuto)}")
                }
            }
        }
    )
}

@Composable
fun DropdownMenuItem(
    label: String,
    selected: String,
    items: List<Pair<String, *>>,
    onSelected: (Any) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(selected.ifEmpty { "Seleziona $label" })
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { (text, value) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onSelected(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun PermRow(nome: String, ok: Boolean, enabled: Boolean = true, onAsk: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            if (ok) Icons.Default.Check else Icons.Default.Warning,
            contentDescription = if (ok) "OK" else "Non concesso"
        )
        Text(nome, Modifier.weight(1f).padding(start = 8.dp))
        if (!ok) {
            TextButton(enabled = enabled, onClick = onAsk) {
                Text("Concedi")
            }
        }
    }
}

fun elencoApp(c: Context): List<Pair<String, String>> {
    val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return c.packageManager.queryIntentActivities(i, 0)
        .mapNotNull {
            val pkg = it.activityInfo?.packageName ?: return@mapNotNull null
            val label = try { it.loadLabel(c.packageManager).toString() } catch (_: Exception) { pkg }
            pkg to label
        }
        .distinctBy { it.first }
        .filter { it.first != c.packageName }
        .sortedBy { it.second.lowercase() }
}
