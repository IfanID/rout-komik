package eu.kanade.presentation.more

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.R

@Composable
fun LogoHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // KMK -->
        // Rout -->
        Image(
            painter = painterResource(R.drawable.rout_transparan),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .padding(vertical = 48.dp)
                .height(80.dp),
        )
        // Rout <--
        // KMK <--

        HorizontalDivider()
    }
}
