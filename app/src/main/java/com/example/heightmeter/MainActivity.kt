package com.example.heightmeter

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.os.Bundle
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.heightmeter.ui.theme.Orange
import com.google.common.util.concurrent.ListenableFuture
import kotlin.math.acos
import kotlin.math.round
import kotlin.math.sqrt
import kotlin.math.tan


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        setContent {
            Surface {
                val context = LocalContext.current
                val isPermissionGranted = remember {
                    mutableStateOf<Boolean?>(
                        ContextCompat.checkSelfPermission(
                            context, Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                }

                val launcher =
                    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                        isPermissionGranted.value = isGranted
                    }

                when (isPermissionGranted.value) {
                    true -> Box {
                        CameraPreview(cameraProviderFuture)
                        ControlsLayout()
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Crosshair(color = Orange)
                        }

                    }
                    else -> TextButton(
                        onClick = { launcher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Text(
                            text = "Tap to grant camera permission",
                            color = androidx.compose.ui.graphics.Color.Black,
                            fontSize = 20.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CameraPreview(cameraProviderFuture: ListenableFuture<ProcessCameraProvider>) {
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(
        factory = { context ->
            PreviewView(context).apply {
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                scaleType = PreviewView.ScaleType.FILL_START
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                post {
                    cameraProviderFuture.addListener(Runnable {
                        val cameraProvider = cameraProviderFuture.get()
                        bindPreview(
                            cameraProvider,
                            lifecycleOwner,
                            this,
                        )
                    }, ContextCompat.getMainExecutor(context))
                }
            }
        }
    )
}
fun bindPreview(cameraProvider: ProcessCameraProvider,
                lifecycleOwner: LifecycleOwner,
                previewView: PreviewView
) {
    val preview = Preview.Builder().build()

    // Connect the preview use case to the previewView
    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    cameraProvider.unbindAll() // Unbind use cases before rebinding
    val camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
    preview.setSurfaceProvider(previewView.surfaceProvider)
}

@Composable
fun CustomButton(
    onClick: () -> Unit,
    label: String
) {
    Button (
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Gray.copy(alpha = 0.6f),
            contentColor = Color.Black,
            disabledContentColor = Color.Black,
            disabledContainerColor = Color.Gray.copy(alpha = 0.4f)
        ),
        modifier = Modifier.width(135.dp),
        border = BorderStroke(width = 1.dp, color = Color.Black)
    ) {
        Text(label)
    }
}


@Composable
fun InputField(
    onValueChange: (String) -> Unit,
    value: String,
    label: String
) {
    val textField = FocusRequester()
    val onChange : (String) -> Unit = { it ->
        val formatted = DecimalFormatter().cleanup(it)
        onValueChange(formatted)
    }
    val focusManager = LocalFocusManager.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(135.dp)
    ) {
        TextField(
            value = value,
            onValueChange = onChange,
            maxLines = 1,
            label = { Text(text = label, color = Orange)},
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions (
                onDone = {
                    focusManager.clearFocus()
                },
            ),
            colors = TextFieldDefaults.colors(
                disabledTextColor = Color.Transparent,
                focusedTextColor = Orange,
                unfocusedTextColor = Orange,
                unfocusedLabelColor = Orange,
                cursorColor = Orange,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                errorContainerColor = Color.Transparent,
                focusedIndicatorColor = Orange,
                unfocusedIndicatorColor = Orange,
                disabledIndicatorColor = Orange,
            ),
            modifier = Modifier.width(125.dp).focusRequester(textField)
        )
    }
}

@Composable
fun Measurement(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(20.dp).width(100.dp)
    ) {
        Text(text = label, color = Orange, fontSize = 25.sp)
        Text(text = value, color = Orange, fontSize = 25.sp)
    }
}

@Composable
fun Crosshair(modifier: Modifier = Modifier, color: Color = Color.Black, size: Dp = 100.dp, strokeWidth: Float = 4f) {
    Canvas(modifier = modifier.size(size)) {
        val canvasWidth = size.toPx()
        val canvasHeight = size.toPx()
        val centerX = canvasWidth / 2
        val centerY = canvasHeight / 2

        // Draw horizontal line
        drawLine(
            color = color,
            start = Offset(x = 0f, y = centerY),
            end = Offset(x = canvasWidth, y = centerY),
            strokeWidth = strokeWidth
        )

        // Draw vertical line
        drawLine(
            color = color,
            start = Offset(x = centerX, y = 0f),
            end = Offset(x = centerX, y = canvasHeight),
            strokeWidth = strokeWidth
        )
    }
}

@Composable
fun ControlsLayout(
) {
    val lensHeight = rememberSaveable { mutableStateOf("") }
    val distance = rememberSaveable { mutableStateOf("") }
    val gravity by rememberSensorValueAsState(
        type = Sensor.TYPE_ACCELEROMETER,
        transformSensorEvent = { event -> event?.values ?: FloatArray(0) },
    )

    fun getHeight(): String {
        if (gravity.isEmpty()) {
            return ""
        }
        val lensHeightValue = try {
            lensHeight.value.toDouble()
        }
        catch (e: Throwable) {
            return ""
        }

        val distanceValue = try {
            distance.value.toDouble()
        }
        catch (e: Throwable) {
            return ""
        }

        try {
            val angle = computeAngle(gravity)
            val height = computeHeight(angle, lensHeightValue, distanceValue)
            return formatHeight(height)
        }
        catch (e: Throwable) {
            return ""
        }
    }

    val height by remember(gravity, lensHeight, distance) { derivedStateOf { getHeight() }}

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
        InputField(
            label = "Enter your height",
            onValueChange = { lensHeight.value = it},
            value = lensHeight.value)
        Measurement(label="Height", value = height)
        InputField(
            label = "Enter Distance",
            onValueChange = { distance.value = it},
            value = distance.value
        )
    }
}

fun formatHeight(height: Double): String {
    val rounded = roundToOneDecimal(height)
    if (rounded < -999.9) {
        return "min"
    }
    if (rounded > 999.9) {
        return "max"
    }
    return rounded.toString()
}

private fun roundToOneDecimal(number: Double): Double {
    return round(number * 10.0) / 10
}

private fun computeHeight(angle: Double, lensHeight: Double, distance: Double): Double {
    return (tan(angle) * distance) + lensHeight
}

private fun computeDistance(angle: Double, lensHeight: Double): Double {
    return lensHeight / (-(tan(angle)))
}

private fun computeAngle(gravity: FloatArray): Double {
    val inclineGravity = gravity.clone()
    val normOfG = sqrt(
        inclineGravity[0] * inclineGravity[0] + inclineGravity[1] * inclineGravity[1] + inclineGravity[2] * inclineGravity[2]
    ) as Float

    // Normalize the accelerometer vector
    inclineGravity[2] = (inclineGravity[2] / normOfG)

    val arcCos = acos(inclineGravity[2]).toDouble()
    val angle = Math.toDegrees(arcCos) - 90.0f
    return Math.toRadians(angle)
}