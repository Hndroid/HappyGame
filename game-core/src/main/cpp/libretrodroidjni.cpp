/*
 *     Copyright (C) 2021  Filippo Scognamiglio
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

#include <jni.h>

#include <EGL/egl.h>

#include <memory>
#include <string>
#include <vector>
#include <unordered_set>
#include <mutex>

#include "libretrodroid.h"
#include "log.h"
#include "core.h"
#include "audio.h"
#include "video.h"
#include "renderers/renderer.h"
#include "fpssync.h"
#include "input.h"
#include "rumble.h"
#include "shadermanager.h"
#include "utils/javautils.h"
#include "errorcodes.h"
#include "environment.h"
#include "renderers/es3/framebufferrenderer.h"
#include "renderers/es2/imagerendereres2.h"
#include "renderers/es3/imagerendereres3.h"
#include "utils/jnistring.h"

using namespace libretrodroid;

extern "C" {
#include "utils/utils.h"
#include "../../libretro-common/include/libretro.h"
#include "utils/libretrodroidexception.h"
}

extern "C" {

jint jni_availableDisks(JNIEnv *env, jobject obj) {
    return LibretroDroid::getInstance().availableDisks();
}

jint jni_currentDisk(JNIEnv *env, jobject obj) {
    return LibretroDroid::getInstance().currentDisk();
}

void jni_changeDisk(JNIEnv *env, jobject obj, jint index) {
    return LibretroDroid::getInstance().changeDisk(index);
}

void jni_updateVariable(JNIEnv *env, jobject obj, jobject variable) {
    Variable v = JavaUtils::variableFromJava(env, variable);
    Environment::getInstance().updateVariable(v.key, v.value);
}

jobjectArray jni_getVariables(JNIEnv *env, jobject obj) {
    jclass variableClass = env->FindClass("com/happy/game/core/Variable");
    jmethodID variableMethodID = env->GetMethodID(variableClass, "<init>", "()V");

    auto variables = Environment::getInstance().getVariables();
    jobjectArray result = env->NewObjectArray(variables.size(), variableClass, nullptr);

    for (int i = 0; i < variables.size(); i++) {
        jobject jVariable = env->NewObject(variableClass, variableMethodID);

        jfieldID jKeyField = env->GetFieldID(variableClass, "key", "Ljava/lang/String;");
        jfieldID jValueField = env->GetFieldID(variableClass, "value", "Ljava/lang/String;");
        jfieldID jDescriptionField = env->GetFieldID(
                variableClass,
                "description",
                "Ljava/lang/String;"
        );

        env->SetObjectField(jVariable, jKeyField, env->NewStringUTF(variables[i].key.data()));
        env->SetObjectField(jVariable, jValueField,
                            env->NewStringUTF(variables[i].value.data()));
        env->SetObjectField(
                jVariable,
                jDescriptionField,
                env->NewStringUTF(variables[i].description.data()));

        env->SetObjectArrayElement(result, i, jVariable);
    }
    return result;
}

jobjectArray
jni_getControllers(JNIEnv *env, jobject obj) {
    jclass variableClass = env->FindClass("[Lcom/happy/game/core/Controller;");

    auto controllers = Environment::getInstance().getControllers();
    jobjectArray result = env->NewObjectArray(controllers.size(), variableClass, nullptr);

    for (int i = 0; i < controllers.size(); i++) {
        jclass variableClass2 = env->FindClass("com/happy/game/core/Controller");
        jobjectArray controllerArray = env->NewObjectArray(
                controllers[i].size(),
                variableClass2,
                nullptr
        );
        jmethodID variableMethodID = env->GetMethodID(variableClass2, "<init>", "()V");

        for (int j = 0; j < controllers[i].size(); j++) {
            jobject jController = env->NewObject(variableClass2, variableMethodID);

            jfieldID jIdField = env->GetFieldID(variableClass2, "id", "I");
            jfieldID jDescriptionField = env->GetFieldID(
                    variableClass2,
                    "description",
                    "Ljava/lang/String;"
            );

            env->SetIntField(jController, jIdField, (int) controllers[i][j].id);
            env->SetObjectField(
                    jController,
                    jDescriptionField,
                    env->NewStringUTF(controllers[i][j].description.data()));

            env->SetObjectArrayElement(controllerArray, j, jController);
        }

        env->SetObjectArrayElement(result, i, controllerArray);
    }
    return result;
}

void jni_setControllerType(JNIEnv *env, jobject obj, jint port, jint type) {
    LibretroDroid::getInstance().setControllerType(port, type);
}

jboolean jni_unserializeState(JNIEnv *env, jobject obj,
                              jbyteArray state) {
    try {
        jboolean isCopy = JNI_FALSE;
        jbyte *data = env->GetByteArrayElements(state, &isCopy);
        jsize size = env->GetArrayLength(state);

        bool result = LibretroDroid::getInstance().unserializeState(data, size);
        env->ReleaseByteArrayElements(state, data, JNI_ABORT);

        return result ? JNI_TRUE : JNI_FALSE;

    } catch (std::exception &exception) {
        LOGE("Error in unserializeState: %s", exception.what());
        JavaUtils::throwRetroException(env, ERROR_SERIALIZATION);
        return JNI_FALSE;
    }
}

jbyteArray jni_serializeState(JNIEnv *env, jobject obj) {
    try {
        auto[data, size] = LibretroDroid::getInstance().serializeState();

        jbyteArray result = env->NewByteArray(size);
        env->SetByteArrayRegion(result, 0, size, data);

        return result;

    } catch (std::exception &exception) {
        LOGE("Error in serializeState: %s", exception.what());
        JavaUtils::throwRetroException(env, ERROR_SERIALIZATION);
    }

    return nullptr;
}

jboolean jni_unserializeSRAM(JNIEnv *env, jobject obj, jbyteArray sram) {
    try {
        jboolean isCopy = JNI_FALSE;
        jbyte *data = env->GetByteArrayElements(sram, &isCopy);
        jsize size = env->GetArrayLength(sram);

        LibretroDroid::getInstance().unserializeSRAM(data, size);

        env->ReleaseByteArrayElements(sram, data, JNI_ABORT);

    } catch (std::exception &exception) {
        LOGE("Error in unserializeSRAM: %s", exception.what());
        JavaUtils::throwRetroException(env, ERROR_SERIALIZATION);
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

jbyteArray jni_serializeSRAM(JNIEnv *env, jobject obj) {
    try {
        auto[data, size] = LibretroDroid::getInstance().serializeSRAM();

        jbyteArray result = env->NewByteArray(size);
        env->SetByteArrayRegion(result, 0, size, (jbyte *) data);

        return result;

    } catch (std::exception &exception) {
        LOGE("Error in serializeSRAM: %s", exception.what());
        JavaUtils::throwRetroException(env, ERROR_SERIALIZATION);
    }

    return nullptr;
}

void jni_reset(JNIEnv *env, jobject obj) {
    try {
        LibretroDroid::getInstance().reset();
    } catch (std::exception &exception) {
        LOGE("Error in clear: %s", exception.what());
        JavaUtils::throwRetroException(env, ERROR_GENERIC);
    }
}

void jni_onSurfaceChanged(JNIEnv *env, jobject obj, jint width, jint height) {
    LibretroDroid::getInstance().onSurfaceChanged(width, height);
}

void jni_onSurfaceCreated(JNIEnv *env, jobject obj) {
    LibretroDroid::getInstance().onSurfaceCreated();
}

void jni_onMotionEvent(JNIEnv *env, jobject obj, jint port, jint source, jfloat xAxis,
                       jfloat yAxis) {
    LibretroDroid::getInstance().onMotionEvent(port, source, xAxis, yAxis);
}

void jni_onKeyEvent(JNIEnv *env, jobject obj, jint port, jint action, jint keyCode) {
    LibretroDroid::getInstance().onKeyEvent(port, action, keyCode);
}

void
jni_create(JNIEnv *env, jobject obj, jint GLESVersion, jstring soFilePath, jstring systemDir,
           jstring savesDir, jobjectArray jVariables,
           jint shaderType, jfloat refreshRate, jboolean preferLowLatencyAudio,
           jboolean enableVirtualFileSystem, jstring language) {
    LOGE("into create: %s", "jni_create");
    try {
        auto corePath = JniString(env, soFilePath).stdString();
        auto deviceLanguage = JniString(env, language).stdString();
        auto systemDirectory = JniString(env, systemDir).stdString();
        auto savesDirectory = JniString(env, savesDir).stdString();

        std::vector<Variable> variables;
        int size = env->GetArrayLength(jVariables);
        for (int i = 0; i < size; i++) {
            auto jVariable = (jobject) env->GetObjectArrayElement(jVariables, i);
            auto variable = JavaUtils::variableFromJava(env, jVariable);
            variables.push_back(variable);
        }

        LibretroDroid::getInstance().create(
                GLESVersion,
                corePath,
                systemDirectory,
                savesDirectory,
                variables,
                shaderType,
                refreshRate,
                preferLowLatencyAudio,
                enableVirtualFileSystem,
                deviceLanguage
        );

    } catch (libretrodroid::LibretroDroidError &exception) {
        LOGE("Error in create: %s", exception.what());
        JavaUtils::throwRetroException(env, exception.getErrorCode());
    } catch (std::exception &exception) {
        LOGE("Error in create: %s", exception.what());
        JavaUtils::throwRetroException(env, ERROR_LOAD_LIBRARY);
    }
}

void jni_loadGameFromPath(JNIEnv *env, jobject obj, jstring gameFilePath) {
    auto gamePath = JniString(env, gameFilePath).stdString();

    try {
        LibretroDroid::getInstance().loadGameFromPath(gamePath);
    } catch (std::exception &exception) {
        LOGE("Error in loadGameFromPath: %s", exception.what());
        JavaUtils::throwRetroException(env, ERROR_LOAD_GAME);
    }
}

void jni_loadGameFromBytes(JNIEnv *env, jobject obj, jbyteArray gameFileBytes) {
    try {
        size_t size = env->GetArrayLength(gameFileBytes);
        auto *data = new int8_t[size];
        env->GetByteArrayRegion(
                gameFileBytes,
                0,
                size,
                reinterpret_cast<int8_t *>(data)
        );
        LibretroDroid::getInstance().loadGameFromBytes(data, size);
    } catch (std::exception &exception) {
        LOGE("Error in loadGameFromBytes: %s", exception.what());
        JavaUtils::throwRetroException(env, ERROR_LOAD_GAME);
    }
}

void *int_loadGameFromVirtualFiles(JNIEnv *env, jobject obj, jobject virtualFileList) {

    try {
        jmethodID getVirtualFileMethodID = env->GetMethodID(
                env->FindClass("com/happy/game/core/DetachedVirtualFile"),
                "getVirtualPath",
                "()Ljava/lang/String;"
        );
        jmethodID getFileDescriptorMethodID = env->GetMethodID(
                env->FindClass("com/happy/game/core/DetachedVirtualFile"),
                "getFileDescriptor",
                "()I"
        );

        std::vector<VFSFile> virtualFiles;

        JavaUtils::javaListForEach(env, virtualFileList, [&](jobject item) {
            JniString virtualFileName(env, (jstring) env->CallObjectMethod(item,
                                                                           getVirtualFileMethodID));
            int fileDescriptor = env->CallIntMethod(item, getFileDescriptorMethodID);
            virtualFiles.emplace_back(VFSFile(virtualFileName.stdString(), fileDescriptor));
        });

        LibretroDroid::getInstance().loadGameFromVirtualFiles(std::move(virtualFiles));
    } catch (std::exception &exception) {
        LOGE("Error in loadGameFromDescriptors: %s", exception.what());
        JavaUtils::throwRetroException(env, ERROR_LOAD_GAME);
    }
}

void jni_destroy(JNIEnv *env, jobject obj) {
    try {
        LibretroDroid::getInstance().destroy();
    } catch (std::exception &exception) {
        LOGE("Error in destroy: %s", exception.what());
        JavaUtils::throwRetroException(env, ERROR_GENERIC);
    }
}

void jni_resume(JNIEnv *env, jobject obj) {
    try {
        LibretroDroid::getInstance().resume();
    } catch (std::exception &exception) {
        LOGE("Error in resume: %s", exception.what());
        JavaUtils::throwRetroException(env, ERROR_GENERIC);
    }
}

void jni_pause(JNIEnv *env, jobject obj) {
    try {
        LibretroDroid::getInstance().pause();
    } catch (std::exception &exception) {
        LOGE("Error in pause: %s", exception.what());
        JavaUtils::throwRetroException(env, ERROR_GENERIC);
    }
}

void jni_step(JNIEnv *env, jobject obj, jobject glRetroView) {
    LibretroDroid::getInstance().step();

    if (LibretroDroid::getInstance().requiresVideoRefresh()) {
        LibretroDroid::getInstance().clearRequiresVideoRefresh();
        jclass cls = env->GetObjectClass(glRetroView);
        jmethodID requestAspectRatioUpdate = env->GetMethodID(cls, "refreshAspectRatio", "()V");
        env->CallVoidMethod(glRetroView, requestAspectRatioUpdate);
    }

    if (LibretroDroid::getInstance().isRumbleEnabled()) {
        LibretroDroid::getInstance().handleRumbleUpdates(
                [&](int port, float weak, float strong) {
                    jclass cls = env->GetObjectClass(glRetroView);
                    jmethodID sendRumbleStrengthMethodID = env->GetMethodID(cls,
                                                                            "sendRumbleEvent",
                                                                            "(IFF)V");
                    env->CallVoidMethod(glRetroView, sendRumbleStrengthMethodID, port, weak,
                                        strong);
                });
    }
}

jfloat jni_getAspectRatio(JNIEnv *env, jobject obj) {
    return LibretroDroid::getInstance().getAspectRatio();
}

void jni_setRumbleEnabled(JNIEnv *env, jobject obj, jboolean enabled) {
    LibretroDroid::getInstance().setRumbleEnabled(enabled);
}

void jni_setFrameSpeed(JNIEnv *env, jobject obj, jint speed) {
    LibretroDroid::getInstance().setFrameSpeed(speed);
}

void jni_setAudioEnabled(JNIEnv *env, jobject obj, jboolean enabled) {
    LibretroDroid::getInstance().setAudioEnabled(enabled);
}

}


#define NELEM(x) ((int)(sizeof(x) / sizeof((x)[0])))
#define JNIREF "com/happy/game/core/HappyFunJni"

static JNINativeMethod method_table[] = {
        {"availableDisks",           "()I",                                                                                                             (void *) jni_availableDisks},
        {"create",                   "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;[Lcom/happy/game/core/Variable;IFZZLjava/lang/String;)V", (void *) jni_create},
        {"loadGameFromPath",         "(Ljava/lang/String;)V",                                                                                           (void *) jni_loadGameFromPath},
        {"loadGameFromBytes",        "([B)V",                                                                                                           (void *) jni_loadGameFromBytes},
        {"loadGameFromVirtualFiles", "(Ljava/util/List;)V",                                                                                             (void *) int_loadGameFromVirtualFiles},
        {"resume",                   "()V",                                                                                                             (void *) jni_resume},
        {"onSurfaceCreated",         "()V",                                                                                                             (void *) jni_onSurfaceCreated},
        {"onSurfaceChanged",         "(II)V",                                                                                                           (void *) jni_onSurfaceChanged},
        {"pause",                    "()V",                                                                                                             (void *) jni_pause},
        {"destroy",                  "()V",                                                                                                             (void *) jni_destroy},
        {"step",                     "(Lcom/happy/game/core/GLRetroView;)V",                                                                            (void *) jni_step},
        {"reset",                    "()V",                                                                                                             (void *) jni_reset},
        {"setRumbleEnabled",         "(Z)V",                                                                                                            (void *) jni_setRumbleEnabled},
        {"setFrameSpeed",            "(I)V",                                                                                                            (void *) jni_setFrameSpeed},
        {"setAudioEnabled",          "(Z)V",                                                                                                            (void *) jni_setAudioEnabled},
        {"serializeState",           "()[B",                                                                                                            (void *) jni_serializeState},
        {"unserializeState",         "([B)Z",                                                                                                           (void *) jni_unserializeState},
        {"serializeSRAM",            "()[B",                                                                                                            (void *) jni_serializeSRAM},
        {"unserializeSRAM",          "([B)Z",                                                                                                           (void *) jni_unserializeSRAM},
        {"updateVariable",           "(Lcom/happy/game/core/Variable;)V",                                                                               (void *) jni_updateVariable},
        {"getVariables",             "()[Lcom/happy/game/core/Variable;",                                                                               (void *) jni_getVariables},
        {"currentDisk",              "()I",                                                                                                             (void *) jni_currentDisk},
        {"changeDisk",               "(I)V",                                                                                                            (void *) jni_changeDisk},
        {"onMotionEvent",            "(IIFF)V",                                                                                                         (void *) jni_onMotionEvent},
        {"onKeyEvent",               "(III)V",                                                                                                          (void *) jni_onKeyEvent},
        {"getAspectRatio",           "()F",                                                                                                             (void *) jni_getAspectRatio},
        {"getControllers",           "()[[Lcom/happy/game/core/Controller;",                                                                            (void *) jni_getControllers},
        {"setControllerType",        "(II)V",                                                                                                           (void *) jni_setControllerType}
};

static int registerNatives(JNIEnv *env) {
    jclass clazz = env->FindClass(JNIREF);
    if (clazz == NULL) {
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, method_table, NELEM(method_table)) < 0) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    LOGD("进入 JNI");
    JNIEnv *env = NULL;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return -1;
    }
    if (!registerNatives(env)) {
        return -1;
    }

    return JNI_VERSION_1_6;
}
