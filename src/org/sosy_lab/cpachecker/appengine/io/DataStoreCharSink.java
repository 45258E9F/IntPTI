/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.appengine.io;

import com.google.common.io.CharSink;
import com.google.common.io.FileWriteMode;

import org.sosy_lab.cpachecker.appengine.entity.TaskFile;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Arrays;


public class DataStoreCharSink extends CharSink {

  private TaskFile file;
  private Charset charset;
  private FileWriteMode[] mode;

  public DataStoreCharSink(TaskFile file, Charset charset, FileWriteMode... mode) {
    this.file = file;
    this.charset = charset;
    this.mode = mode;
  }

  @Override
  public Writer openStream() throws IOException {
    if (charset == null) {
      charset = Charset.defaultCharset();
    }

    Writer writer = file.getContentWriter(charset);

    // write any existing content to the writer to enable appending
    if (Arrays.asList(mode).contains(FileWriteMode.APPEND)) {
      writer.write(file.getContent());
    }

    return writer;
  }
}
