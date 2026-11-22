package com.neo10.gamecube.os7;

import android.content.Context;

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
            String safePkg = (pkg == null || pkg.isEmpty()) ? "default" : pkg;
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
                    safePkg, safePkg, "vivo",
                    9, false,
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
        if (lpparam == null || lpparam.packageName == null) {
            return;
        }
        if ("com.vivo.gamecube".equals(lpparam.packageName)) {
            hookGameCube(lpparam.classLoader);
        } else if ("com.vivo.gamewatch".equals(lpparam.packageName)) {
            hookGameWatch(lpparam.classLoader);
        }
    }

    private void hookGameCube(final ClassLoader cl) {
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

        // 1. Hardware & Feature Flags (No H0 so built-in fan UI stays hidden)
        String commonA = "com.vivo.common.a";
        safeHook(cl, commonA, "z0", retTrue);
        safeHook(cl, commonA, "I1", retTrue);
        safeHook(cl, commonA, "J1", retTrue);
        safeHook(cl, commonA, "o1", retTrue);
        safeHook(cl, commonA, "R0", retTrue);
        safeHook(cl, commonA, "S0", retTrue);

        // 2. Monster+ Sidebar + Auto Monster+ on Game Launch
        String msPkg = "com.vivo.gameassistant.homegui.sideslide.panels.performance.modeshift";
        safeHook(cl, msPkg + ".a", "a", retTrue);
        safeHook(cl, msPkg + ".o", "j", retTrue);
        safeHook(cl, msPkg + ".o", "m", retTrue);
        safeHook(cl, msPkg + ".o", "o", retTrue);
        safeHook(cl, msPkg + ".ModeShiftBar", "f0", boolean.class, setArg0True);
        safeHook(cl, msPkg + ".ModeShiftBar", "K", boolean.class, boolean.class, setArg0True);

        try {
            final Class<?> modeTypeCls = XposedHelpers.findClass(msPkg + ".ModeType", cl);
            final Object monsterPlus = XposedHelpers.getStaticObjectField(modeTypeCls, "MONSTER_PLUS");
            final Object monster = XposedHelpers.getStaticObjectField(modeTypeCls, "MONSTER");
            final Object enhanced = XposedHelpers.getStaticObjectField(modeTypeCls, "ENHANCED");

            safeHook(cl, msPkg + ".a", "e", XC_MethodReplacement.returnConstant(monsterPlus));

            safeHook(cl, msPkg + ".o", "g", String.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Object res = param.getResult();
                    if (res == monster || res == enhanced) {
                        param.setResult(monsterPlus);
                    }
                }
            });

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

        // 3. 144 FPS Unlock with Q2 Hardware gameIndex = 9
        String utilsI0 = "com.vivo.gameassistant.utils.i0";
        safeHook(cl, utilsI0, "v0", String.class, retTrue);
        safeHook(cl, utilsI0, "w0", String.class, retTrue);
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

        String[] ofkMethods = {"b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l"};
        for (String m : ofkMethods) {
            safeHook(cl, "of.k", m, String.class, retTrue);
        }

        XC_MethodHook synthCdbHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (param.getResult() == null) {
                    try {
                        Class<?> infoCls = XposedHelpers.findClass("cd.b", cl);
                        Object info = XposedHelpers.newInstance(infoCls);
                        XposedHelpers.setIntField(info, "h", 9);
                        XposedHelpers.setBooleanField(info, "i", false);
                        param.setResult(info);
                    } catch (Throwable t) {
                        XposedBridge.log("[Neo10OS7] cd.b synth err: " + t.getMessage());
                    }
                }
            }
        };
        safeHook(cl, "of.i", "e", String.class, synthCdbHook);
        safeHook(cl, "of.j", "e", String.class, synthCdbHook);

        XC_MethodHook frameArrayFallbackHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                Object res = param.getResult();
                boolean empty = (res == null) || (res instanceof int[] && ((int[]) res).length < 2);
                if (empty) {
                    Object state = param.args[4];
                    if (state != null && state.toString().contains("OPTIMIZE_POWER")) {
                        param.setResult(new int[]{60, 120, 60});
                    } else {
                        param.setResult(new int[]{72, 144, 72});
                    }
                }
            }
        };
        safeHook(cl, "of.i", "d",
                Context.class, int.class, int.class, int.class,
                "com.vivo.gameassistant.frameinterpolation.FrameInterpolationState",
                frameArrayFallbackHook);
        safeHook(cl, "of.j", "d",
                Context.class, int.class, int.class, int.class,
                "com.vivo.gameassistant.frameinterpolation.FrameInterpolationState",
                frameArrayFallbackHook);

        safeHook(cl, "of.j", "y", String.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (param.getResult() == null) {
                    String pkg = (param.args != null && param.args.length > 0 && param.args[0] instanceof String)
                            ? (String) param.args[0] : "";
                    Object synth = build144FrameProfile(cl, pkg);
                    if (synth != null) {
                        param.setResult(synth);
                    }
                }
            }
        });
        safeHook(cl, "of.j", "j", String.class, retTrue);
        safeHook(cl, "of.j", "k", String.class, retTrue);
        safeHook(cl, "of.j", "l", String.class, retFalse);
        safeHook(cl, "of.h", "p", String.class, retTrue);
        safeHook(cl, "of.h", "q", String.class, retTrue);
        safeHook(cl, "of.h", "s", String.class, retFalse);

        String fiH = "com.vivo.gameassistant.frameinterpolation.h";
        String fiI = "com.vivo.gameassistant.frameinterpolation.i";
        safeHook(cl, fiH, "W0", retFalse);
        safeHook(cl, fiH, "Q0", retFalse);
        safeHook(cl, fiI, "g", XC_MethodReplacement.returnConstant(new int[]{48, 144}));
        safeHook(cl, fiI, "m", String.class, retTrue);

        try {
            Class<?> fiStateCls = XposedHelpers.findClass(
                    "com.vivo.gameassistant.frameinterpolation.FrameInterpolationState", cl);
            safeHook(cl, fiH, "z0", fiStateCls, String.class, ret144);
            safeHook(cl, fiH, "B0", fiStateCls, String.class,
                    XC_MethodReplacement.returnConstant(new int[]{72, 144}));
        } catch (Throwable t) {
            XposedBridge.log("[Neo10OS7] FIState skip: " + t.getMessage());
        }

        // 4. Thermal Overheat Bypass in GameCube
        safeHook(cl, fiH, "X0", double.class, retFalse);
        safeHook(cl, fiH, "V0", retTrue);
        try {
            Class<?> reasonCls = XposedHelpers.findClass(
                    "com.vivo.gameassistant.frameinterpolation.ChangeFrameReason", cl);
            safeHook(cl, fiH, "u0", reasonCls, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    Object reason = param.args[0];
                    if (reason != null && reason.toString().contains("OVERHEAT")) {
                        param.setResult(null);
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log("[Neo10OS7] Thermal bypass skip: " + t.getMessage());
        }

        XposedBridge.log("[Neo10OS7] v5 GameCube hooks loaded.");
    }

    private void hookGameWatch(final ClassLoader cl) {
        final XC_MethodReplacement retTrue = XC_MethodReplacement.returnConstant(Boolean.TRUE);
        final XC_MethodReplacement retFalse = XC_MethodReplacement.returnConstant(Boolean.FALSE);

        try {
            Class<?> reqCls = XposedHelpers.findClass("Lj0.l$c", cl);
            safeHook(cl, "Lj0.l", "V", reqCls, retTrue);
            safeHook(cl, "Lj0.l", "N", reqCls, retTrue);
        } catch (Throwable t) {
            XposedBridge.log("[Neo10OS7] GameWatch l$c skip: " + t.getMessage());
        }

        safeHook(cl, "Lj0.l", "a0", int.class, retTrue);
        safeHook(cl, "Lj0.l", "f0", int.class, retTrue);

        // Block GameWatch thermal FPS & Q2 MEMC throttling
        safeHook(cl, "Lj0.l", "y0", double.class, boolean.class, retFalse);
        safeHook(cl, "Lj0.z0", "p3", double.class, boolean.class, retFalse);
        safeHook(cl, "Lj0.z0", "u3", double.class, boolean.class, retFalse);

        XposedBridge.log("[Neo10OS7] v5 GameWatch Q2 MEMC & thermal hooks loaded.");
    }
}
