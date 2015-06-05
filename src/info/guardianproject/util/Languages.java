
package info.guardianproject.util;

import android.app.Activity;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class Languages {
    private static final String TAG = "Languages";
    private static Languages singleton;
    private static Map<String, String> tmpMap = new TreeMap<String, String>();
    private static Map<String, String> nameMap;
    public static final String USE_SYSTEM_DEFAULT = "";
    public static final Locale TIBETAN = new Locale("bo");
    static final Locale localesToTest[] = {
            Locale.ENGLISH, Locale.FRENCH, Locale.GERMAN,
            Locale.ITALIAN, Locale.JAPANESE, Locale.KOREAN,
            Locale.TRADITIONAL_CHINESE, Locale.SIMPLIFIED_CHINESE,
            TIBETAN, new Locale("af"), new Locale("am"),
            new Locale("ar"), new Locale("az"), new Locale("bg"),
            new Locale("bn"), new Locale("ca"), new Locale("cs"),
            new Locale("da"), new Locale("el"), new Locale("es"),
            new Locale("et"), new Locale("eu"), new Locale("fa"),
            new Locale("fi"), new Locale("gl"), new Locale("hi"),
            new Locale("hr"), new Locale("hu"), new Locale("hy"),
            new Locale("in"), new Locale("hy"), new Locale("in"),
            new Locale("is"), new Locale("it"), new Locale("iw"),
            new Locale("ka"), new Locale("kk"), new Locale("km"),
            new Locale("kn"), new Locale("ky"), new Locale("lo"),
            new Locale("lt"), new Locale("lv"), new Locale("mk"),
            new Locale("ml"), new Locale("mn"), new Locale("mr"),
            new Locale("ms"), new Locale("my"), new Locale("nb"),
            new Locale("ne"), new Locale("nl"), new Locale("pl"),
            new Locale("pt"), new Locale("rm"), new Locale("ro"),
            new Locale("ru"), new Locale("si"), new Locale("sk"),
            new Locale("sl"), new Locale("sn"), new Locale("sr"),
            new Locale("sv"), new Locale("sw"), new Locale("ta"),
            new Locale("te"), new Locale("th"), new Locale("tl"),
            new Locale("tr"), new Locale("uk"), new Locale("ur"),
            new Locale("uz"), new Locale("vi"), new Locale("zu"),
    };

    private Languages(Activity activity, int resId, String defaultString) {
        AssetManager assets = activity.getAssets();
        Configuration config = activity.getResources().getConfiguration();
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        Resources resources;
        Set<Locale> localeSet = new LinkedHashSet<Locale>();
        for (Locale locale : localesToTest) {
            config.locale = locale;
            resources = new Resources(assets, metrics, config);
            if (!TextUtils.equals(defaultString, resources.getString(resId))
                    || locale.equals(Locale.ENGLISH))
                localeSet.add(locale);
        }
        for (Locale locale : localeSet) {
            if (locale.equals(TIBETAN)) {
                // include English name for devices that don't support Tibetan
                // font
                tmpMap.put(TIBETAN.getLanguage(), "Tibetan བོད་སྐད།"); // Tibetan
            } else if (locale.equals(Locale.SIMPLIFIED_CHINESE)) {
                tmpMap.put(Locale.SIMPLIFIED_CHINESE.toString(), "中文 (中国)"); // Chinese
                                                                             // (China)
            } else if (locale.equals(Locale.TRADITIONAL_CHINESE)) {
                tmpMap.put(Locale.TRADITIONAL_CHINESE.toString(), "中文 (台灣)"); // Chinese
                                                                              // (Taiwan)
            } else {
                tmpMap.put(locale.getLanguage(), locale.getDisplayLanguage(locale));
            }
        }
        // TODO implement this completely, the menu item works, but doesn't work
        // properly
        /* USE_SYSTEM_DEFAULT is a fake one for displaying in a chooser menu. */
        // localeSet.add(null);
        // tmpMap.put(USE_SYSTEM_DEFAULT,
        // activity.getString(R.string.use_system_default));
        nameMap = Collections.unmodifiableMap(tmpMap);
    }

    /**
     * Get the instance of {@link Languages} to work with, providing the
     * {@link Activity} that is will be working as part of. This uses the
     * provided string resource {@code resId} find the supported translations:
     * if an included translation has a translated string that matches that
     * {@code resId}, i.e. {@code R.string.menu_settings}, then that language
     * will be included as a supported language.
     * 
     * @param activity the {@link Activity} this is working as part of
     * @param resId the string resource ID to test, e.g.
     *            {@code R.string.menu_settings}
     * @param defaultString the string resource in the default language, e.g.
     *            {@code "Settings"}
     * @return
     */
    public static Languages get(Activity activity, int resId, String defaultString) {
        if (singleton == null)
            singleton = new Languages(activity, resId, defaultString);
        return singleton;
    }

    /**
     * Return the name of the language based on the locale.
     * 
     * @param locale
     * @return
     */
    public String getName(String locale) {
        String ret = nameMap.get(locale);
        // if no match, try to return a more general name (i.e. English for
        // en_IN)
        if (ret == null && locale.contains("_"))
            ret = nameMap.get(locale.split("_")[0]);
        return ret;
    }

    /**
     * Return an array of the names of all the supported languages, sorted to
     * match what is returned by {@link Languages#getSupportedLocales()}.
     * 
     * @return
     */
    public String[] getAllNames() {
        return nameMap.values().toArray(new String[nameMap.size()]);
    }

    public int getPosition(Locale locale) {
        String localeName = locale.getLanguage();
        int i = 0;
        for (String key : nameMap.keySet())
            if (TextUtils.equals(key, localeName))
                return i;
            else
                i++;
        return -1;
    }

    /**
     * Get sorted list of supported locales.
     * 
     * @return
     */
    public String[] getSupportedLocales() {
        Set<String> keys = nameMap.keySet();
        return keys.toArray(new String[keys.size()]);
    }
}
