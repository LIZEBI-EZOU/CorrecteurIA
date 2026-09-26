package com.correcteur.ia
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{CorrecteurApp()}}}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun CorrecteurApp(vm:CorrecteurViewModel=viewModel()){
    val state by vm.uiState.collectAsState()
    MaterialTheme{Scaffold(topBar={TopAppBar(title={Column{Text("Correcteur IA",fontWeight=FontWeight.Bold);Text("Correction privée • hors connexion",style=MaterialTheme.typography.labelSmall)}})}){padding->
        Column(Modifier.padding(padding).padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){
            OutlinedTextField(value=state.input,onValueChange=vm::setInput,modifier=Modifier.fillMaxWidth().heightIn(min=220.dp),label={Text("Votre texte")},placeholder={Text("Écrivez ou collez votre texte ici…")})
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick=vm::correct,enabled=state.input.isNotBlank()&&!state.busy){Text("✓ Corriger")};OutlinedButton(onClick=vm::rewrite,enabled=state.input.isNotBlank()&&!state.busy){Text("✨ Reformuler")}}
            if(state.busy)LinearProgressIndicator(Modifier.fillMaxWidth())
            if(state.output.isNotBlank()){Text("Résultat",style=MaterialTheme.typography.titleMedium);Card(Modifier.fillMaxWidth()){Text(state.output,Modifier.padding(16.dp),style=MaterialTheme.typography.bodyLarge)};Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick=vm::useResult){Text("Remplacer")};OutlinedButton(onClick=vm::clearOutput){Text("Effacer")}}}
            Text(state.message,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.secondary)
            HorizontalDivider();Text("Mode IA",style=MaterialTheme.typography.titleMedium);Text("La correction de base reste locale. L’IA on-device est utilisée automatiquement sur les appareils compatibles.",style=MaterialTheme.typography.bodyMedium)
        }
    }}
}
