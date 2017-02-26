package arun.com.chromer.customtabs.dynamictoolbar;

import android.app.IntentService;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.graphics.Palette;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import arun.com.chromer.R;
import arun.com.chromer.data.apps.AppRepository;
import arun.com.chromer.util.ColorUtil;
import arun.com.chromer.util.Utils;
import timber.log.Timber;

import static android.content.pm.PackageManager.GET_META_DATA;
import static arun.com.chromer.shared.Constants.EXTRA_PACKAGE_NAME;
import static arun.com.chromer.shared.Constants.NO_COLOR;


public class AppColorExtractorService extends IntentService {

    public AppColorExtractorService() {
        super(AppColorExtractorService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME);
            if (packageName != null) {
                if (isValidPackage(packageName))
                    return;

                if (!extractColorFromResources(packageName)) {
                    extractColorFromAppIcon(packageName);
                }
            }
        }
    }

    private boolean extractColorFromResources(@NonNull final String packageName) {
        try {
            int color;
            final Resources resources = getPackageManager().getResourcesForApplication(packageName);
            // Try to extract appcompat primary color value
            int appCompatId = resources.getIdentifier("colorPrimary", "attr", packageName);
            if (appCompatId > 0) {
                // Successful, let's get the themed value of this attribute
                color = getThemedColor(resources, appCompatId, packageName);
                if (color != NO_COLOR) {
                    saveColorToDb(packageName, color);
                    return true;
                }
            }
            // If above was not successful, then attempt to get lollipop colorPrimary attribute
            int lollipopAttrId = resources.getIdentifier("android:colorPrimary", "attr", packageName);
            if (lollipopAttrId > 0) {
                // Found
                color = getThemedColor(resources, lollipopAttrId, packageName);
                if (color != NO_COLOR) {
                    saveColorToDb(packageName, color);
                    return true;
                }
            }
            return false;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    private int getThemedColor(@Nullable Resources resources, int attributeId, @NonNull String packageName)
            throws PackageManager.NameNotFoundException {
        if (resources == null || attributeId == 0) return -1;
        // Create dummy theme
        final Resources.Theme tempTheme = resources.newTheme();
        // Need the theme id to apply the theme, so let's get it.
        int themeId = getPackageManager().getPackageInfo(packageName, GET_META_DATA).applicationInfo.theme;
        // Apply the theme
        tempTheme.applyStyle(themeId, false);
        // Attempt to get styled values now
        final TypedArray array = tempTheme.obtainStyledAttributes(new int[]{attributeId});
        // Styled color
        int color = array.getColor(0, NO_COLOR);
        array.recycle();
        if (color == ContextCompat.getColor(this, R.color.md_grey_100)
                || color == ContextCompat.getColor(this, R.color.md_grey_900)) {
            color = NO_COLOR;
        }
        return color;
    }

    private void extractColorFromAppIcon(@NonNull final String packageName) {
        try {
            final Bitmap iconBitmap = Utils.drawableToBitmap(getPackageManager().getApplicationIcon(packageName));
            final Palette palette = Palette.from(iconBitmap)
                    .clearFilters()
                    .generate();
            //noinspection ConstantConditions
            int extractColor = getPreferredColorFromSwatches(palette);
            if (extractColor != NO_COLOR) {
                Timber.d("Extracted %d for %s", extractColor, packageName);
                try {
                    saveColorToDb(packageName, extractColor);
                    iconBitmap.recycle();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void saveColorToDb(final String packageName, @ColorInt int extractedColor) {
        AppRepository.getInstance(this)
                .setPackageColor(packageName, extractedColor)
                .subscribe();
    }

    private boolean isValidPackage(String app) {
        return app.equalsIgnoreCase(getPackageName()) || app.equalsIgnoreCase("android") || app.isEmpty();
    }

    private int getPreferredColorFromSwatches(Palette palette) {
        final List<Palette.Swatch> swatchList = ColorUtil.getSwatchListFromPalette(palette);
        final Palette.Swatch prominentSwatch = Collections.max(swatchList,
                new Comparator<Palette.Swatch>() {
                    @Override
                    public int compare(Palette.Swatch swatch1, Palette.Swatch swatch2) {
                        int a = swatch1 == null ? 0 : swatch1.getPopulation();
                        int b = swatch2 == null ? 0 : swatch2.getPopulation();
                        return a - b;
                    }
                });
        if (prominentSwatch != null)
            return prominentSwatch.getRgb();
        else return -1;
    }

}