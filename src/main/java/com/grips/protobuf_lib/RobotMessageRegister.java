/*
 *
 * Copyright (c) 2017, Graz Robust and Intelligent Production System (grips)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package com.grips.protobuf_lib;

import com.google.protobuf.Descriptors;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.GeneratedMessageV3;
import org.robocup_logistics.llsf_utils.Key;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class RobotMessageRegister {
    private static RobotMessageRegister _instance;
    private HashMap<Key, GeneratedMessageV3> _msgs = new HashMap<>();

    private RobotMessageRegister() {
    }

    public static RobotMessageRegister getInstance() {
        return (_instance == null) ? _instance = new RobotMessageRegister() : _instance;
    }

    public <T extends GeneratedMessageV3> void add_message(Class<T> c) {
        try {
            Method m = c.getMethod("getDefaultInstance", (Class<?>[]) null);
            T msg = (T) m.invoke(null, (Object[]) null);
            Descriptors.EnumDescriptor desc = msg.getDescriptorForType().findEnumTypeByName("CompType");

            int cmp_id = desc.findValueByName("COMP_ID").getNumber();
            int msg_id = desc.findValueByName("MSG_TYPE").getNumber();
            Key key = new Key(cmp_id, msg_id);
            _msgs.put(key, msg);
            System.out.println("Registering msg with COMP_ID: " + cmp_id + " and MSG_TYPE " + msg_id + "\t(" + c.getSimpleName() + ").");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    public GeneratedMessageV3 get_generated_empty_msg_from_key(int cmp_id, int msg_id) {
        for (Map.Entry<Key, GeneratedMessageV3> e : _msgs.entrySet()) {
            Key key = e.getKey();
            if (key.cmp_id == cmp_id && key.msg_id == msg_id) {
                return e.getValue();
            }
        }
        System.err.println("ERROR: Not registered message to be parsed in RobotMessageRegister.");
        System.err.println("ERROR: Message with cmp_id " + cmp_id + " and msg_id " + msg_id + " is not registered.");
        return null;
    }

    public GeneratedMessageV3 get_generated_empty_msg_from_key(Key key) {
        return get_generated_empty_msg_from_key(key.cmp_id, key.msg_id);
    }

    public <T extends GeneratedMessageV3> Key get_msg_key_from_class(Class<T> c) {
        try {
            Method m = c.getMethod("getDefaultInstance", (Class<?>[]) null);
            T msg = (T) m.invoke((Object[]) null, (Object[]) null);
            Descriptors.EnumDescriptor desc = msg.getDescriptorForType().findEnumTypeByName("CompType");
            int cmp_id = desc.findValueByName("COMP_ID").getNumber();
            int msg_id = desc.findValueByName("MSG_TYPE").getNumber();
            return new Key(cmp_id, msg_id);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        System.err.println("ERROR: Can not get key from msg.");
        return null;
    }
}
