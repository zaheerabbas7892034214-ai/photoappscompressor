import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview

private val LightColorScheme = lightColorScheme(
    primary = ... // Define your colors here
    onPrimary = ... // Define your colors here
    // Add other colors as needed
)

@Composable
fun PhotoCompressorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewPhotoCompressorTheme() {
    PhotoCompressorTheme { 
        // Preview your UI here
    }
}