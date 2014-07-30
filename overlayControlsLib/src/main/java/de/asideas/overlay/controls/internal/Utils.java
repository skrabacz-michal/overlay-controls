package de.asideas.overlay.controls.internal;

import android.app.Service;
import android.content.Context;
import android.graphics.Point;
import android.util.TypedValue;
import android.view.Display;
import android.view.WindowManager;

/**
 * Created by mskrabacz on 09/07/14.
 */
public class Utils
{
    private Utils()
    {
        // Empty
    }

    public static int dpToPx(Context context, int value)
    {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics());
    }

    public static Point getScreenCenterPoint(Context context)
    {
        Display display = ((WindowManager) context.getSystemService(Service.WINDOW_SERVICE)).getDefaultDisplay();
        final Point size = new Point();
        display.getSize(size);

        return size;
    }
}
