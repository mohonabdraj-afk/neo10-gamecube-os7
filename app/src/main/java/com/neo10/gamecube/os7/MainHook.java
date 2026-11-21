package com.neo10.gamecube.os7;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    private static void safeHook(ClassLoader cl, String cls, String method, Object... args) {
        try {
            XposedHelpers.findAndHookMethod(cls, cl, method, args);
        } catch (Throwable t) {
            XposedBridge.log("[Neo10OS7] Skip " + cls + "." + method + ": " + t.getMessage());
        }
    }

    private static boolean isFrameFunction(Object configuredFunc) {
        if (configuredFunc == null) {
            return false;
        }
        String name = configuredFunc.toString();
        return name.contains("FRAME") || name.contains("OPTIMIZE_POWER");
    }

    private static Object build144FrameProfile(ClassLoader cl, String pkg) {
        try {
            Class<?> valCls = XposedHelpers.findClass("yb.b", cl);
            Constructor<?> valCtor = valCls.getConstructor(int.class, int.class, int.class);

            List<Object> boostList = new ArrayList<>();
            boostList.add(valCtor.newInstance(60, 48, 144));
            boostList.add(valCtor.newInstance(90, 72, 144));
            boostList.add(valCtor.newInstance(120, 72, 144));

            List<Object> optiList = new ArrayList<>();
            optiList.add(valCtor.newInstance(60, 45, 90));
            optiList.add(valCtor.newInstance(90, 60, 120));

            Class<?> entityCls = XposedHelpers.findClass(
                    "com.vivo.common.frameintersupportlist.entity.b", cl);
            Constructor<?> ctor = entityCls.getConstructor(
                    String.class, String.class, String.class,
                    int.class, boolean.class,
                    List.class, List.class, Map.class,
                    double.class, double.class,
                    int.class, int.class,
                    String.class, int.class, int.class, int.class, String.class
            );
            return ctor.newInstance(
                    pkg, pkg, "vivo",
                    0, false,
                    boostList, optiList, new HashMap<Integer, String>(),
                    85.0d, 75.0d,
                    0, 0,
                    "", 144, 72, 144, "fixed"
            );
        } catch (Throwable t) {
            XposedBridge.log("[Neo10OS7] Profile synth err: " + t.getMessage());
            return null;
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (lpparam == null || !"com.vivo.gamecube".equals(lpparam.packageName)) {
            return;
        }

        final ClassLoader cl = lpparam.classLoader;
        final XC_MethodReplacement retTrue = XC_MethodReplacement.returnConstant(Boolean.TRUE);
        final XC_MethodReplacement retFalse = XC_MethodReplacement.returnConstant(Boolean.FALSE);
        final XC_MethodReplacement ret144 = XC_MethodReplacement.returnConstant(144);

        final XC_MethodHook setArg0True = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (param.args != null && param.args.length > 0) {
                    param.args[0] = Boolean.TRUE;
                }
            }
        };

        // ============================================================
        // 1. Hardware & Feature Flags (OriginOS 7 com.vivo.common.a)
        // ============================================================
        String commonA = "com.vivo.common.a";
        safeHook(cl, commonA, "z0", retTrue); // ro.vivo.support_monster_plus
        safeHook(cl, commonA, "I1", retTrue); // monster plus support cached
        safeHook(cl, commonA, "J1", retTrue); // monster_plus_combine
        safeHook(cl, commonA, "H0", retTrue); // cooling fan support
        safeHook(cl, commonA, "o1", retTrue); // vivo.hardware.game.memc
        safeHook(cl, commonA, "R0", retTrue); // support_fps
        safeHook(cl, commonA, "S0", retTrue); // monsterplus_opt

        // ============================================================
        // 2. Monster+ Sidebar + Auto Monster+ on Game Launch
        // ============================================================
        String msPkg = "com.vivo.gameassistant.homegui.sideslide.panels.performance.modeshift";
        safeHook(cl, msPkg + ".a", "a", retTrue);  // cooler connected = true
        safeHook(cl, msPkg + ".o", "j", retTrue);  // back clip state = true
        safeHook(cl, msPkg + ".o", "m", retTrue);  // already opened Monster+ = true
        safeHook(cl, msPkg + ".o", "o", retTrue);  // monster+ combine active = true
        safeHook(cl, msPkg + ".ModeShiftBar", "f0", boolean.class, setArg0True);
        safeHook(cl, msPkg + ".ModeShiftBar", "K", boolean.class, boolean.class, setArg0True);

        try {
            final Class<?> modeTypeCls = XposedHelpers.findClass(msPkg + ".ModeType", cl);
            final Object monsterPlus = XposedHelpers.getStaticObjectField(modeTypeCls, "MONSTER_PLUS");
            final Object monster = XposedHelpers.getStaticObjectField(modeTypeCls, "MONSTER");
            final Object enhanced = XposedHelpers.getStaticObjectField(modeTypeCls, "ENHANCED");

            // Sidebar plus button always maps to MONSTER_PLUS
            safeHook(cl, msPkg + ".a", "e", XC_MethodReplacement.returnConstant(monsterPlus));

            // Auto-upgrade saved MONSTER/ENHANCED mode -> MONSTER_PLUS on game launch
            safeHook(cl, msPkg + ".o", "g", String.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Object res = param.getResult();
                    if (res == monster || res == enhanced) {
                        param.setResult(monsterPlus);
                    }
                }
            });

            // Upgrade MONSTER/ENHANCED shift calls -> MONSTER_PLUS automatically
            safeHook(cl, msPkg + ".o", "w", modeTypeCls, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    Object target = param.args[0];
                    if (target == monster || target == enhanced) {
                        param.args[0] = monsterPlus;
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log("[Neo10OS7] Auto Monster+ hook skip: " + t.getMessage());
        }

        // ============================================================
        // 3. 144 FPS Quick Switch in Game Tools + Q Chip Zone Unlock
        // ============================================================
        String utilsI0 = "com.vivo.gameassistant.utils.i0";
        safeHook(cl, utilsI0, "v0", String.class, retTrue); // isGameSupportFrameInter
        safeHook(cl, utilsI0, "w0", String.class, retTrue); // isGameSupportIris
        try {
            Class<?> cfgFuncCls = XposedHelpers.findClass(
                    "com.vivo.common.supportlist.pojo.ConfiguredFunction", cl);
            XC_MethodHook frameFuncTrue = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.args != null && param.args.length > 0 && isFrameFunction(param.args[0])) {
                        param.setResult(Boolean.TRUE);
                    }
                }
            };
            safeHook(cl, utilsI0, "W0", cfgFuncCls, String.class, frameFuncTrue);
            safeHook(cl, utilsI0, "p0", cfgFuncCls, String.class, frameFuncTrue);
        } catch (Throwable t) {
            XposedBridge.log("[Neo10OS7] ConfiguredFunction skip: " + t.getMessage());
        }

        // Exact OriginOS 7 classes: of.j (support list DB), of.h (V3 strategy)
        safeHook(cl, "of.j", "y", String.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (param.getResult() == null && param.args[0] instanceof String) {
                    String pkg = (String) param.args[0];
                    if (!pkg.isEmpty()) {
                        Object synth = build144FrameProfile(cl, pkg);
                        if (synth != null) {
                            param.setResult(synth);
                        }
                    }
                }
            }
        });
        safeHook(cl, "of.j", "j", String.class, retTrue);
        safeHook(cl, "of.j", "k", String.class, retTrue);
        safeHook(cl, "of.j", "l", String.class, retFalse); // not restricted to Monster+ only
        safeHook(cl, "of.h", "p", String.class, retTrue);
        safeHook(cl, "of.h", "q", String.class, retTrue);
        safeHook(cl, "of.h", "s", String.class, retFalse);

        // FrameInterpolationController (h) & Helper (i)
        String fiH = "com.vivo.gameassistant.frameinterpolation.h";
        String fiI = "com.vivo.gameassistant.frameinterpolation.i";
        safeHook(cl, fiH, "W0", retFalse); // bypass Monster+ requirement in SwitchPresenter
        safeHook(cl, fiH, "Q0", retFalse);
        safeHook(cl, fiI, "g", XC_MethodReplacement.returnConstant(new int[]{48, 144}));
        safeHook(cl, fiI, "m", String.class, retTrue);

        try {
            Class<?> fiStateCls = XposedHelpers.findClass(
                    "com.vivo.gameassistant.frameinterpolation.FrameInterpolationState", cl);
            safeHook(cl, fiH, "z0", fiStateCls, String.class, ret144);
            safeHook(cl, fiH, "B0", fiStateCls, String.class,
                    XC_MethodReplacement.returnConstant(new int[]{60, 144}));
        } catch (Throwable t) {
            XposedBridge.log("[Neo10OS7] FIState skip: " + t.getMessage());
        }

        // ============================================================
        // 4. Thermal Overheat Bypass (Stops GameCube shutting down 144 FPS)
        // ============================================================
        safeHook(cl, fiH, "X0", double.class, retFalse); // isOverheat -> false
        safeHook(cl, fiH, "V0", retTrue);                // skip temp check -> true
        try {
            Class<?> reasonCls = XposedHelpers.findClass(
                    "com.vivo.gameassistant.frameinterpolation.ChangeFrameReason", cl);
            safeHook(cl, fiH, "u0", reasonCls, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    Object reason = param.args[0];
                    if (reason != null && reason.toString().contains("OVERHEAT")) {
                        param.setResult(null); // Block overheat shutdown only
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log("[Neo10OS7] Thermal bypass skip: " + t.getMessage());
        }

        XposedBridge.log("[Neo10OS7] v2 Auto Monster+ & 144FPS hooks loaded cleanly.");
    }
}
