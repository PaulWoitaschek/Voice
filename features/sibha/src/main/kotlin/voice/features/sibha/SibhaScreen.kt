@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package voice.features.sibha

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.launch
import voice.core.common.rootGraphAs
import voice.core.strings.R as StringsR
import voice.core.ui.R as UiR

@Composable
fun SibhaScreen(modifier: Modifier = Modifier) {
  val graph = remember { rootGraphAs<SibhaGraph>() }
  val countStore = graph.sibhaCountStore
  val reminderStore = graph.sibhaReminderStore
  val count by remember(countStore) { countStore.data }.collectAsState(initial = 0)
  val reminderEvery by remember(reminderStore) { reminderStore.data }.collectAsState(initial = 0)
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  var reminderInput by rememberSaveable { mutableStateOf("0") }
  var showResetConfirmation by rememberSaveable { mutableStateOf(false) }

  LaunchedEffect(reminderEvery) {
    reminderInput = reminderEvery.toString()
  }

  BackHandler { graph.navigator.goBack() }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = { Text(stringResource(StringsR.string.sibha_title)) },
        navigationIcon = {
          IconButton(onClick = graph.navigator::goBack) {
            Icon(
              painter = painterResource(UiR.drawable.ic_arrow_back),
              contentDescription = stringResource(StringsR.string.common_action_close),
            )
          }
        },
      )
    },
  ) { contentPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(contentPadding)
          .padding(horizontal = 20.dp, vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
      ) {
        Column(
          modifier = Modifier.padding(horizontal = 20.dp, vertical = 28.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(
            text = stringResource(StringsR.string.sibha_counter_label),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = count.toString(),
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 68.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Button(
            onClick = {
              scope.launch {
                runCatching {
                  countStore.updateData { current ->
                    if (current == Int.MAX_VALUE) current else current + 1
                  }
                }.onSuccess { updatedCount ->
                  if (shouldPlaySibhaReminder(updatedCount, reminderEvery)) {
                    playReminderFeedback(context)
                  }
                }.onFailure {
                  snackbarHostState.showSnackbar(
                    context.getString(StringsR.string.sibha_error_save_count)
                  )
                }
              }
            },
            modifier = Modifier.size(184.dp),
            shape = CircleShape,
            contentPadding = PaddingValues(16.dp),
          ) {
            Text(
              text = stringResource(StringsR.string.sibha_tap_to_count),
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.SemiBold,
            )
          }
          Text(
            text = stringResource(StringsR.string.sibha_tap_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          OutlinedButton(onClick = { showResetConfirmation = true }) {
            Text(stringResource(StringsR.string.sibha_reset))
          }
        }
      }

      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(
            text = stringResource(StringsR.string.sibha_reminder_title),
            style = MaterialTheme.typography.titleLarge,
          )
          Text(
            text = stringResource(StringsR.string.sibha_reminder_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          OutlinedTextField(
            value = reminderInput,
            onValueChange = { value ->
              if (value.length <= 5 && value.all(Char::isDigit)) reminderInput = value
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(StringsR.string.sibha_reminder_interval)) },
            supportingText = {
              Text(stringResource(StringsR.string.sibha_reminder_hint))
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          )
          val reminderValue = reminderInput.toIntOrNull()
          val validReminder = reminderValue != null && reminderValue in 0..10_000
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            TextButton(
              onClick = {
                reminderInput = "0"
                scope.launch { reminderStore.updateData { 0 } }
              },
            ) {
              Text(stringResource(StringsR.string.sibha_reminder_turn_off))
            }
            Spacer(Modifier.weight(1f))
            Button(
              enabled = validReminder,
              onClick = {
                val value = reminderValue ?: return@Button
                scope.launch {
                  runCatching { reminderStore.updateData { value } }
                    .onSuccess {
                      snackbarHostState.showSnackbar(
                        context.getString(StringsR.string.sibha_reminder_saved)
                      )
                    }
                    .onFailure {
                      snackbarHostState.showSnackbar(
                        context.getString(StringsR.string.sibha_error_save_reminder)
                      )
                    }
                }
              },
            ) {
              Text(stringResource(StringsR.string.sibha_reminder_save))
            }
          }
          Text(
            text =
              if (reminderEvery == 0) {
                stringResource(StringsR.string.sibha_reminder_status_off)
              } else {
                stringResource(StringsR.string.sibha_reminder_status_on, reminderEvery)
              },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }

  if (showResetConfirmation) {
    AlertDialog(
      onDismissRequest = { showResetConfirmation = false },
      title = { Text(stringResource(StringsR.string.sibha_reset_title)) },
      text = { Text(stringResource(StringsR.string.sibha_reset_message)) },
      confirmButton = {
        TextButton(
          onClick = {
            showResetConfirmation = false
            scope.launch {
              runCatching { countStore.updateData { 0 } }
                .onFailure {
                  snackbarHostState.showSnackbar(
                    context.getString(StringsR.string.sibha_error_reset)
                  )
                }
            }
          },
        ) {
          Text(stringResource(StringsR.string.sibha_reset))
        }
      },
      dismissButton = {
        TextButton(onClick = { showResetConfirmation = false }) {
          Text(stringResource(StringsR.string.common_action_cancel))
        }
      },
    )
  }
}

@Suppress("DEPRECATION")
private fun playReminderFeedback(context: Context) {
  runCatching {
    val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
    tone.startTone(ToneGenerator.TONE_PROP_BEEP, 140)
    Handler(Looper.getMainLooper()).postDelayed({ runCatching { tone.release() } }, 250L)
  }

  runCatching {
    val effect = VibrationEffect.createOneShot(60L, VibrationEffect.DEFAULT_AMPLITUDE)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      context.getSystemService(VibratorManager::class.java)?.defaultVibrator?.vibrate(effect)
    } else {
      val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
      if (vibrator != null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          vibrator.vibrate(effect)
        } else {
          vibrator.vibrate(60L)
        }
      }
    }
  }
}
