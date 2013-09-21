// Copyright 2012 Cloudera Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.cloudera.impala.analysis;

import com.cloudera.impala.catalog.PrimitiveType;
import com.cloudera.impala.common.AnalysisException;
import com.cloudera.impala.thrift.TColumnType;
import com.google.common.base.Preconditions;

/**
 * Class wrapping a type of a column. For most types, this will just be a wrapper
 * around an enum but for types like CHAR(n) and decimal, this will contain additional
 * information.
 * TODO: this should replace PrimitiveType everywhere in analysis.
 */
public class ColumnType {
  private final PrimitiveType type_;

  // Unused if type_ is always the same length.
  private int len_;

  private ColumnType(PrimitiveType type) {
    type_ = type;
  }

  public static ColumnType createType(PrimitiveType type) {
    Preconditions.checkState(type != PrimitiveType.CHAR);
    return new ColumnType(type);
  }

  public static ColumnType createCharType(int len) {
    ColumnType type = new ColumnType(PrimitiveType.CHAR);
    type.len_ = len;
    return type;
  }

  public final PrimitiveType getType() { return type_; }
  public int getSlotSize() {
    switch (type_) {
    case CHAR:
      return len_;
    default:
      return type_.getSlotSize();
    }
  }

  public void analyze() throws AnalysisException {
    if (type_ == PrimitiveType.CHAR) {
      if (len_ <= 0) {
        throw new AnalysisException("Array size must be > 0. Size was set to: " +
            len_ + ".");
      }
    }
  }

  @Override
  public String toString() { return type_.toString(); }

  public TColumnType toThrift() {
    TColumnType thrift = new TColumnType();
    thrift.type = type_.toThrift();
    if (type_ == PrimitiveType.CHAR) thrift.setLen(len_);
    return thrift;
  }

  public static ColumnType fromThrift(TColumnType thrift) {
    PrimitiveType type = PrimitiveType.fromThrift(thrift.type);
    if (type == PrimitiveType.CHAR) {
      Preconditions.checkState(thrift.isSetLen());
      return createCharType(thrift.len);
    } else {
      return createType(type);
    }
  }
}
