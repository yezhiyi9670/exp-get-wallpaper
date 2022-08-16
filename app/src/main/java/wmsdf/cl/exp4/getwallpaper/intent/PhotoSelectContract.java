package wmsdf.cl.exp4.getwallpaper.intent;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.activity.result.contract.ActivityResultContract;

public class PhotoSelectContract extends ActivityResultContract<java.lang.Integer, Uri> {

    @Override
    public Intent createIntent(Context context, java.lang.Integer input) {
        return new Intent(Intent.ACTION_PICK).setType("image/*");
    }

    @Override
    public Uri parseResult(int resultCode, Intent intent) {
        if(intent == null) {
            return null;
        }
        return intent.getData();
    }

}
