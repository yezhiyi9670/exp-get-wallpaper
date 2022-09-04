package wmsdf.cl.exp4.getwallpaper.kotlin.intent

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract

class PhotoSelectContract : ActivityResultContract<Unit, Uri?>() {

    override fun createIntent(context: Context, input: Unit): Intent {
        return Intent(Intent.ACTION_PICK).setType("image/*")
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        if(intent == null) {
            return null
        }
        return intent.data
    }

}