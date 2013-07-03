/**
 * Copyright (C) 2012 https://github.com/yelbota/haxe-maven-plugin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yelbota.plugins.haxe.utils;

import org.codehaus.plexus.logging.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class CleanStream extends Thread {
private String myname = "stream";
    public enum CleanStreamType {
        INFO, DEBUG, ERROR     
    }
    
    private InputStream is;
    private CleanStreamType type = null;
    private Logger log = null;
    private int count = 0;

    public CleanStream(InputStream is)
    {
        this.is = is;
    }

    public CleanStream(InputStream is, Logger log)
    {
        this.is = is;
        this.log = log;
    }

    public CleanStream(InputStream is, Logger log, CleanStreamType type)
    {
        this.is = is;
        this.type = type;
        this.log = log;
    }

    public CleanStream(InputStream is, Logger log, CleanStreamType type, String myname)
    {
        this.is = is;
        this.type = type;
        this.log = log;
        this.myname = myname;
    }

    public int getCount()
    {
        return this.count;
    }

    public void run()
    {
        try
        {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line = null;

            while ((line = br.readLine()) != null)
            {
                count++;
                if (log != null)
                {
                    if (type == null) {

                        log.info(line);

                    }
                    else {
                        if (type == CleanStreamType.INFO) {
                            log.info(line);
                        } else if (type == CleanStreamType.DEBUG) {
                            log.debug(line);
                        } else if (type == CleanStreamType.ERROR) {
                            if (line.matches("(.*)[Ww]arning(.*)")
                                    || !line.matches("(.*)[Ee]rror(.*)")) {
                                log.warn(line);
                            } else {
                                log.error("("+myname+") " + line);
                            }
                        }
                    }
                }
            }
        }
        catch (IOException ioe)
        {
            ioe.printStackTrace();
        }
    }
}