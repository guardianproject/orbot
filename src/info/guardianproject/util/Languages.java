package info.guardianproject.util;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class Languages {
    public static final String TAG = "Languages";

    public static final Locale defaultLocale;
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

    private static final String USE_SYSTEM_DEFAULT = "";
    private static final String defaultString = "Use System Default";

    private static Locale locale;
    private static Languages singleton;
    private static Class<?> clazz;
    private static int resId;
    private static Map<String, String> tmpMap = new TreeMap<String, String>();
    private static Map<String, String> nameMap;

    static {
        defaultLocale = Locale.getDefault();
    }

    private Languages(Activity activity) {
        AssetManager assets = activity.getAssets();
        Configuration config = activity.getResources().getConfiguration();
        // Resources() requires DisplayMetrics, but they are only needed for drawables
        DisplayMetrics ignored = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(ignored);
        Resources resources;
        Set<Locale> localeSet = new LinkedHashSet<Locale>();
        for (Locale locale : localesToTest) {
            config.locale = locale;
            resources = new Resources(assets, ignored, config);
            if (!TextUtils.equals(defaultString, resources.getString(resId))
                    || locale.equals(Locale.ENGLISH))
                localeSet.add(locale);
        }
        for (Locale locale : localeSet) {
            if (locale.equals(TIBETAN)) {
                // include English name for devices without Tibetan font support
                tmpMap.put(TIBETAN.getLanguage(), "Tibetan བོད་སྐད།"); // Tibetan
            } else if (locale.equals(Locale.SIMPLIFIED_CHINESE)) {
                tmpMap.put(Locale.SIMPLIFIED_CHINESE.toString(), "中文 (中国)"); // Chinese (China)
            } else if (locale.equals(Locale.TRADITIONAL_CHINESE)) {
                tmpMap.put(Locale.TRADITIONAL_CHINESE.toString(), "中文 (台灣)"); // Chinese (Taiwan)
            } else {
                tmpMap.put(locale.getLanguage(), locale.getDisplayLanguage(locale));
            }
        }

        /* USE_SYSTEM_DEFAULT is a fake one for displaying in a chooser menu. */
        localeSet.add(null);
        tmpMap.put(USE_SYSTEM_DEFAULT, activity.getString(resId));
        nameMap = Collections.unmodifiableMap(tmpMap);
    }

    /**
     * Get the instance of {@link Languages} to work with, providing the
     * {@link Activity} that is will be working as part of, as well as the
     * {@code resId} that has the exact string "Use System Default",
     * i.e. {@code R.string.use_system_default}.
     * <p/>
     * That string resource {@code resId} is also used to find the supported
     * translations: if an included translation has a translated string that
     * matches that {@code resId}, then that language will be included as a
     * supported language.
     *
     * @param clazz the {@link Class} of the default {@code Activity},
     *              usually the main {@code Activity} from where the
     *              Settings is launched from.
     * @param resId the string resource ID to for the string "Use System Default",
     *              e.g. {@code R.string.use_system_default}
     * @return
     */
    public static void setup(Class<?> clazz, int resId) {
        if (Languages.clazz == null) {
            Languages.clazz = clazz;
            Languages.resId = resId;
        } else {
            throw new RuntimeException("Languages singleton was already initialized, duplicate call to Languages.setup()!");
        }
    }

    /**
     * Get the singleton to work with.
     *
     * @param activity the {@link Activity} this is working as part of
     * @return
     */
    public static Languages get(Activity activity) {
        if (singleton == null) {
            singleton = new Languages(activity);
        }
        return singleton;
    }

    //@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @SuppressLint("NewApi")
	public static void setLanguage(final ContextWrapper contextWrapper, String language, boolean refresh) {
        if (locale != null && TextUtils.equals(locale.getLanguage(), language) && (!refresh)) {
            return; // already configured
        } else if (language == null || language == USE_SYSTEM_DEFAULT) {
            locale = defaultLocale;
        } else {
            /* handle locales with the country in it, i.e. zh_CN, zh_TW, etc */
            String localeSplit[] = language.split("_");
            if (localeSplit.length > 1) {
                locale = new Locale(localeSplit[0], localeSplit[1]);
            } else {
                locale = new Locale(language);
            }
        }

        final Resources resources = contextWrapper.getBaseContext().getResources();
        Configuration config = resources.getConfiguration();
        if (Build.VERSION.SDK_INT >= 17) {
            config.setLocale(locale);
        } else {
            config.locale = locale;
        }
        resources.updateConfiguration(config, resources.getDisplayMetrics());
        Locale.setDefault(locale);

    }

    /**
     * Force reload the {@link Activity to make language changes take effect.}
     *
     * @param activity the {@code Activity} to force reload
     */
    public static void forceChangeLanguage(Activity activity) {
        Intent intent = activity.getIntent();
        if (intent == null) // when launched as LAUNCHER
            return;
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        activity.finish();
        activity.overridePendingTransition(0, 0);
        activity.startActivity(intent);
        activity.overridePendingTransition(0, 0);
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
