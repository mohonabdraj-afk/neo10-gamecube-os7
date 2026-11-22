package com.neo10.gamecube.os7;

import android.content.Context;
import android.net.Uri;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
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

        String commonA = "com.vivo.common.a";
        safeHook(cl, commonA, "z0", retTrue);
        safeHook(cl, commonA, "I1", retTrue);
        safeHook(cl, commonA, "J1", retTrue);
        safeHook(cl, commonA, "o1", retTrue);
        safeHook(cl, commonA, "R0", retTrue);
        safeHook(cl, commonA, "S0", retTrue);

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

        XposedBridge.log("[Neo10OS7] v9 GameCube hooks loaded cleanly.");
    }

    private void hookGameWatch(final ClassLoader cl) {
        final XC_MethodReplacement retTrue = XC_MethodReplacement.returnConstant(Boolean.TRUE);
        final XC_MethodReplacement retFalse = XC_MethodReplacement.returnConstant(Boolean.FALSE);
        final XC_MethodReplacement retOne = XC_MethodReplacement.returnConstant(1);
        final XC_MethodReplacement retZero = XC_MethodReplacement.returnConstant(0);
        final XC_MethodReplacement retZeroLong = XC_MethodReplacement.returnConstant(0L);

        // 1. PicassoUseCaseXmlParser (h0.g): Enable EMEMC, IMEMC, GPUMemc and disable Passthrough
        safeHook(cl, "h0.g", "c", String.class, retOne);
        safeHook(cl, "h0.g", "g", String.class, retOne);
        safeHook(cl, "h0.g", "e", String.class, retOne);
        safeHook(cl, "h0.g", "s", String.class, retOne);
        safeHook(cl, "h0.g", "o", String.class, retZero);
        safeHook(cl, "h0.g", "x", String.class, retTrue);

        // 2. Stop GameWatch from clamping CPU speed via QUOTA_DIFF_CLAMP (34004480)
        safeHook(cl, "p2.h", "g", int.class, int[].class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (param.args != null && param.args.length > 0 && param.args[0] instanceof Integer) {
                    int cmd = (Integer) param.args[0];
                    if (cmd == 34004480) {
                        param.setResult(0);
                    }
                }
            }
        });

        // 3. ThermalFpsAdjuster (m0.f): Prevent GameWatch thermal FPS cap
        try {
            Class<?> tfItemCls = XposedHelpers.findClass("m0.f$a", cl);
            safeHook(cl, "m0.f", "k", tfItemCls, retZeroLong);
        } catch (Throwable t) {
            XposedBridge.log("[Neo10OS7] m0.f skip: " + t.getMessage());
        }

        // 4. InterpolatedFpsAdjuster (j0.l - Q2 Chip) & SrAndMemcManagerQ3 (j0.z0)
        try {
            final Class<?> reqCls = XposedHelpers.findClass("j0.l$c", cl);

            // Q2 Chip core hooks (allow all games, enable low-latency FtFlags, disable Genshin 73Hz quirk)
            safeHook(cl, "j0.l", "Y", int.class, int.class, reqCls, retTrue);
            safeHook(cl, "j0.l", "V", reqCls, retTrue);
            safeHook(cl, "j0.l", "N", reqCls, retTrue);
            safeHook(cl, "j0.l", "c0", reqCls, retTrue);
            safeHook(cl, "j0.l", "W", reqCls, retFalse);
            safeHook(cl, "j0.l", "a0", int.class, retTrue);
            safeHook(cl, "j0.l", "f0", int.class, retTrue);
            safeHook(cl, "j0.l", "y0", double.class, boolean.class, retFalse);

            // Q3/Q4 fallback manager hooks (j0.z0)
            final Class<?> z0Cls = XposedHelpers.findClass("j0.z0", cl);
            safeHook(cl, "j0.z0", "X1", int.class, int.class, reqCls, retTrue);
            safeHook(cl, "j0.z0", "b2", int.class, retTrue);
            safeHook(cl, "j0.z0", "p3", double.class, boolean.class, retFalse);
            safeHook(cl, "j0.z0", "u3", double.class, boolean.class, retFalse);

            safeHook(cl, "j0.z0", "f3", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    Object self = param.thisObject;
                    if (self == null) {
                        return;
                    }
                    try {
                        Object reqM = XposedHelpers.getObjectField(self, "M");
                        if (reqM != null && XposedHelpers.getIntField(reqM, "a") == 1) {
                            param.setResult(null);
                        }
                    } catch (Throwable ignored) {
                    }
                }
            });

            safeHook(cl, "j0.z0", "s2", Uri.class, boolean.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    Object self = param.thisObject;
                    if (self == null) {
                        return;
                    }
                    try {
                        XposedHelpers.setStaticBooleanField(z0Cls, "a2", true);
                        XposedHelpers.setStaticBooleanField(z0Cls, "V1", true);
                        XposedHelpers.setBooleanField(self, "Z0", true);
                        XposedHelpers.setBooleanField(self, "b1", true);
                        XposedHelpers.setBooleanField(self, "e1", true);
                        XposedHelpers.setBooleanField(self, "J0", false);
                        XposedHelpers.setIntField(self, "w1", 144);

                        int m1 = XposedHelpers.getStaticIntField(z0Cls, "M1");
                        if (m1 > 90) {
                            XposedHelpers.setStaticIntField(z0Cls, "M1", 90);
                        }
                    } catch (Throwable t) {
                        XposedBridge.log("[Neo10OS7] z0.s2 field patch err: " + t.getMessage());
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log("[Neo10OS7] GameWatch j0.l/j0.z0 hook skip: " + t.getMessage());
        }

        // 5. Diagnostic log for Q2 Hardware IC dispatch (e0.a.o type=258)
        safeHook(cl, "e0.a", "o", int.class, int[].class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (param.args != null && param.args.length >= 2 && param.args[0] instanceof Integer) {
                    int type = (Integer) param.args[0];
                    if (type == 258) {
                        int[] vals = (param.args[1] instanceof int[]) ? (int[]) param.args[1] : null;
                        XposedBridge.log("[Neo10OS7] Q2 IC cmd type=258 vals="
                                + Arrays.toString(vals) + " ret=" + param.getResult());
                    }
                }
            }
        });

        XposedBridge.log("[Neo10OS7] v9 GameWatch Q2 MEMC + ThermalFps + CPU-Clamp hooks loaded cleanly.");
    }
}
