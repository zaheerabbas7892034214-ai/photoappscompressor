package com.photocompressor.kbsize

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.photocompressor.kbsize.ui.theme.PhotoCompressorTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var billingManager: BillingManager
    private lateinit var interstitialAdManager: InterstitialAdManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        billingManager = BillingManager(applicationContext)
        billingManager.initialize(applicationContext)

        interstitialAdManager = InterstitialAdManager(this)

        setContent {
            PhotoCompressorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PhotoCompressorScreen(
                        billingManager = billingManager,
                        interstitialAdManager = interstitialAdManager,
                        activity = this
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        billingManager.destroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoCompressorScreen(
    billingManager: BillingManager,
    interstitialAdManager: InterstitialAdManager,
    activity: ComponentActivity
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Billing states
    val isPro by billingManager.isPro.collectAsStateWithLifecycle()
    val productPrice by billingManager.productPrice.collectAsStateWithLifecycle()
    val billingReady by billingManager.billingReady.collectAsStateWithLifecycle()
    val lastError by billingManager.lastError.collectAsStateWithLifecycle()

    // UI states
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedTargetSize by remember { mutableStateOf(100) }
    var customTargetSize by remember { mutableStateOf("") }
    var isCompressing by remember { mutableStateOf(false) }
    var compressionResult by remember { mutableStateOf<ImageCompressor.Result?>(null) }
    var savedImageUri by remember { mutableStateOf<Uri?>(null) }

    val imageCompressor = remember { ImageCompressor() }
    val mediaStoreSaver = remember { MediaStoreSaver() }

    // Image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        compressionResult = null
        savedImageUri = null
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            imagePickerLauncher.launch("image/*")
        } else {
            Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // Check and request permission
    fun checkAndRequestPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED -> {
                imagePickerLauncher.launch("image/*")
            }
            else -> {
                permissionLauncher.launch(permission)
            }
        }
    }

    // Compress image
    fun compressImage() {
        val uri = selectedImageUri ?: return
        val targetKB = customTargetSize.toIntOrNull() ?: selectedTargetSize

        if (targetKB <= 0) {
            Toast.makeText(context, "Invalid target size", Toast.LENGTH_SHORT).show()
            return
        }

        isCompressing = true
        savedImageUri = null

        scope.launch {
            val result = imageCompressor.compressToTargetSize(context, uri, targetKB)
            isCompressing = false

            result.onSuccess { compResult ->
                compressionResult = compResult
                Toast.makeText(
                    context,
                    "Compressed to ${compResult.compressedSizeKB}KB",
                    Toast.LENGTH_SHORT
                ).show()

                // Show interstitial ad for free users
                if (!isPro) {
                    interstitialAdManager.showAd(activity)
                }
            }.onFailure { error ->
                Toast.makeText(
                    context,
                    "Compression failed: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // Save compressed image
    fun saveImage() {
        val result = compressionResult ?: return

        scope.launch {
            val saveResult = mediaStoreSaver.saveImage(context, result.compressedData)
            saveResult.onSuccess { uri ->
                savedImageUri = uri
                Toast.makeText(context, "Image saved successfully", Toast.LENGTH_SHORT).show()
            }.onFailure { error ->
                Toast.makeText(
                    context,
                    "Failed to save: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // Share compressed image
    fun shareImage() {
        val uri = savedImageUri
        if (uri == null) {
            Toast.makeText(context, "Please save the image first", Toast.LENGTH_SHORT).show()
            return
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share compressed image"))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text("Photo Compressor - KB Size") },
            actions = {
                TextButton(onClick = { billingManager.restorePurchases() }) {
                    Text("Restore Purchases")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Show billing error if any
            lastError?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Pro status indicator
            if (isPro) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = "✓ Pro Version Active",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Image preview
            selectedImageUri?.let { uri ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(uri),
                        contentDescription = "Selected image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Select image button
            Button(
                onClick = { checkAndRequestPermission() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (selectedImageUri == null) "Select Image" else "Change Image")
            }

            // Target size section
            if (selectedImageUri != null) {
                Text(
                    text = "Select Target Size",
                    style = MaterialTheme.typography.titleMedium
                )

                // Preset chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(50, 100, 200, 500, 1000).forEach { size ->
                        FilterChip(
                            selected = selectedTargetSize == size && customTargetSize.isEmpty(),
                            onClick = {
                                selectedTargetSize = size
                                customTargetSize = ""
                            },
                            label = { Text(if (size >= 1000) "${size / 1000}MB" else "${size}KB") }
                        )
                    }
                }

                // Custom size input
                OutlinedTextField(
                    value = customTargetSize,
                    onValueChange = { customTargetSize = it },
                    label = { Text("Custom size (KB)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Compress button
                Button(
                    onClick = { compressImage() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCompressing
                ) {
                    if (isCompressing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isCompressing) "Compressing..." else "Compress Image")
                }
            }

            // Results display
            compressionResult?.let { result ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Compression Results",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Original: ${result.originalSizeKB} KB",
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Compressed: ${result.compressedSizeKB} KB",
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Reduction: ${result.compressionPercentage}%",
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Dimensions: ${result.finalWidth} x ${result.finalHeight}",
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                // Save and Share buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { saveImage() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }
                    Button(
                        onClick = { shareImage() },
                        modifier = Modifier.weight(1f),
                        enabled = savedImageUri != null
                    ) {
                        Text("Share")
                    }
                }
            }

            // Upgrade to Pro button
            if (!isPro && billingReady) {
                Button(
                    onClick = { billingManager.purchasePro(activity) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Text("Upgrade to Pro ${productPrice ?: ""}")
                }
                Text(
                    text = "Remove ads and support development",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Banner ad at bottom (only for free users)
        if (!isPro) {
            Spacer(modifier = Modifier.weight(1f))
            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}
