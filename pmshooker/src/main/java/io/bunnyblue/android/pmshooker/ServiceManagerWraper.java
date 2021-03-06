package io.bunnyblue.android.pmshooker;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

/**copy from https://github.com/fourbrother/HookPmsSignature
 * Created by jiangwei1-g on 2016/9/7.
 */
public class ServiceManagerWraper {

    public static void hookPMS(Context context, String signed, int hashCode){
        try{
            // 获取全局的ActivityThread对象
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Method currentActivityThreadMethod = 
            		activityThreadClass.getDeclaredMethod("currentActivityThread");
            Object currentActivityThread = currentActivityThreadMethod.invoke(null);
            // 获取ActivityThread里面原始的sPackageManager
            Field sPackageManagerField = activityThreadClass.getDeclaredField("sPackageManager");
            sPackageManagerField.setAccessible(true);
            Object sPackageManager = sPackageManagerField.get(currentActivityThread);
            // 准备好代理对象, 用来替换原始的对象
            FakePackageManger fakePackageManger=new FakePackageManger(context.getPackageManager());
            Class<?> iPackageManagerInterface = Class.forName("android.content.pm.IPackageManager");
            Object proxy = Proxy.newProxyInstance(
                    iPackageManagerInterface.getClassLoader(),
                    new Class<?>[] { iPackageManagerInterface },
                    new PmsHookBinderInvocationHandler(sPackageManager, signed, hashCode));
            // 1. 替换掉ActivityThread里面的 sPackageManager 字段
            sPackageManagerField.set(currentActivityThread, proxy);//proxy
            // 2. 替换 ApplicationPackageManager里面的 mPM对象
            PackageManager pm = context.getPackageManager();
            Field mPmField = pm.getClass().getDeclaredField("mPM");
            mPmField.setAccessible(true);
            mPmField.set(pm, proxy);//proxy
        }catch (Exception e){
            Log.d("jw", "hook pms error:"+Log.getStackTraceString(e));
        }
    }
    
}
