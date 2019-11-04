// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: VersionInfo.proto

package org.robocup_logistics.llsf_msgs;

public final class VersionProtos {
  private VersionProtos() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
  }
  public interface VersionInfoOrBuilder extends
      // @@protoc_insertion_point(interface_extends:llsf_msgs.VersionInfo)
      com.google.protobuf.MessageOrBuilder {

    /**
     * <code>required uint32 version_major = 1;</code>
     */
    boolean hasVersionMajor();
    /**
     * <code>required uint32 version_major = 1;</code>
     */
    int getVersionMajor();

    /**
     * <code>required uint32 version_minor = 2;</code>
     */
    boolean hasVersionMinor();
    /**
     * <code>required uint32 version_minor = 2;</code>
     */
    int getVersionMinor();

    /**
     * <code>required uint32 version_micro = 3;</code>
     */
    boolean hasVersionMicro();
    /**
     * <code>required uint32 version_micro = 3;</code>
     */
    int getVersionMicro();

    /**
     * <code>required string version_string = 4;</code>
     */
    boolean hasVersionString();
    /**
     * <code>required string version_string = 4;</code>
     */
    String getVersionString();
    /**
     * <code>required string version_string = 4;</code>
     */
    com.google.protobuf.ByteString
        getVersionStringBytes();
  }
  /**
   * Protobuf type {@code llsf_msgs.VersionInfo}
   */
  public static final class VersionInfo extends
      com.google.protobuf.GeneratedMessage implements
      // @@protoc_insertion_point(message_implements:llsf_msgs.VersionInfo)
      VersionInfoOrBuilder {
    // Use VersionInfo.newBuilder() to construct.
    private VersionInfo(com.google.protobuf.GeneratedMessage.Builder<?> builder) {
      super(builder);
      this.unknownFields = builder.getUnknownFields();
    }
    private VersionInfo(boolean noInit) { this.unknownFields = com.google.protobuf.UnknownFieldSet.getDefaultInstance(); }

    private static final VersionInfo defaultInstance;
    public static VersionInfo getDefaultInstance() {
      return defaultInstance;
    }

    public VersionInfo getDefaultInstanceForType() {
      return defaultInstance;
    }

    private final com.google.protobuf.UnknownFieldSet unknownFields;
    @Override
    public final com.google.protobuf.UnknownFieldSet
        getUnknownFields() {
      return this.unknownFields;
    }
    private VersionInfo(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      initFields();
      int mutable_bitField0_ = 0;
      com.google.protobuf.UnknownFieldSet.Builder unknownFields =
          com.google.protobuf.UnknownFieldSet.newBuilder();
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            default: {
              if (!parseUnknownField(input, unknownFields,
                                     extensionRegistry, tag)) {
                done = true;
              }
              break;
            }
            case 8: {
              bitField0_ |= 0x00000001;
              versionMajor_ = input.readUInt32();
              break;
            }
            case 16: {
              bitField0_ |= 0x00000002;
              versionMinor_ = input.readUInt32();
              break;
            }
            case 24: {
              bitField0_ |= 0x00000004;
              versionMicro_ = input.readUInt32();
              break;
            }
            case 34: {
              com.google.protobuf.ByteString bs = input.readBytes();
              bitField0_ |= 0x00000008;
              versionString_ = bs;
              break;
            }
          }
        }
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(this);
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(
            e.getMessage()).setUnfinishedMessage(this);
      } finally {
        this.unknownFields = unknownFields.build();
        makeExtensionsImmutable();
      }
    }
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return org.robocup_logistics.llsf_msgs.VersionProtos.internal_static_llsf_msgs_VersionInfo_descriptor;
    }

    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return org.robocup_logistics.llsf_msgs.VersionProtos.internal_static_llsf_msgs_VersionInfo_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              org.robocup_logistics.llsf_msgs.VersionProtos.VersionInfo.class, org.robocup_logistics.llsf_msgs.VersionProtos.VersionInfo.Builder.class);
    }

    public static com.google.protobuf.Parser<VersionInfo> PARSER =
        new com.google.protobuf.AbstractParser<VersionInfo>() {
      public VersionInfo parsePartialFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws com.google.protobuf.InvalidProtocolBufferException {
        return new VersionInfo(input, extensionRegistry);
      }
    };

    @Override
    public com.google.protobuf.Parser<VersionInfo> getParserForType() {
      return PARSER;
    }

    /**
     * Protobuf enum {@code llsf_msgs.VersionInfo.CompType}
     */
    public enum CompType
        implements com.google.protobuf.ProtocolMessageEnum {
      /**
       * <code>COMP_ID = 2000;</code>
       */
      COMP_ID(0, 2000),
      /**
       * <code>MSG_TYPE = 3;</code>
       */
      MSG_TYPE(1, 3),
      ;

      /**
       * <code>COMP_ID = 2000;</code>
       */
      public static final int COMP_ID_VALUE = 2000;
      /**
       * <code>MSG_TYPE = 3;</code>
       */
      public static final int MSG_TYPE_VALUE = 3;


      public final int getNumber() { return value; }

      public static CompType valueOf(int value) {
        switch (value) {
          case 2000: return COMP_ID;
          case 3: return MSG_TYPE;
          default: return null;
        }
      }

      public static com.google.protobuf.Internal.EnumLiteMap<CompType>
          internalGetValueMap() {
        return internalValueMap;
      }
      private static com.google.protobuf.Internal.EnumLiteMap<CompType>
          internalValueMap =
            new com.google.protobuf.Internal.EnumLiteMap<CompType>() {
              public CompType findValueByNumber(int number) {
                return CompType.valueOf(number);
              }
            };

      public final com.google.protobuf.Descriptors.EnumValueDescriptor
          getValueDescriptor() {
        return getDescriptor().getValues().get(index);
      }
      public final com.google.protobuf.Descriptors.EnumDescriptor
          getDescriptorForType() {
        return getDescriptor();
      }
      public static final com.google.protobuf.Descriptors.EnumDescriptor
          getDescriptor() {
        return org.robocup_logistics.llsf_msgs.VersionProtos.VersionInfo.getDescriptor().getEnumTypes().get(0);
      }

      private static final CompType[] VALUES = values();

      public static CompType valueOf(
          com.google.protobuf.Descriptors.EnumValueDescriptor desc) {
        if (desc.getType() != getDescriptor()) {
          throw new IllegalArgumentException(
            "EnumValueDescriptor is not for this type.");
        }
        return VALUES[desc.getIndex()];
      }

      private final int index;
      private final int value;

      private CompType(int index, int value) {
        this.index = index;
        this.value = value;
      }

      // @@protoc_insertion_point(enum_scope:llsf_msgs.VersionInfo.CompType)
    }

    private int bitField0_;
    public static final int VERSION_MAJOR_FIELD_NUMBER = 1;
    private int versionMajor_;
    /**
     * <code>required uint32 version_major = 1;</code>
     */
    public boolean hasVersionMajor() {
      return ((bitField0_ & 0x00000001) == 0x00000001);
    }
    /**
     * <code>required uint32 version_major = 1;</code>
     */
    public int getVersionMajor() {
      return versionMajor_;
    }

    public static final int VERSION_MINOR_FIELD_NUMBER = 2;
    private int versionMinor_;
    /**
     * <code>required uint32 version_minor = 2;</code>
     */
    public boolean hasVersionMinor() {
      return ((bitField0_ & 0x00000002) == 0x00000002);
    }
    /**
     * <code>required uint32 version_minor = 2;</code>
     */
    public int getVersionMinor() {
      return versionMinor_;
    }

    public static final int VERSION_MICRO_FIELD_NUMBER = 3;
    private int versionMicro_;
    /**
     * <code>required uint32 version_micro = 3;</code>
     */
    public boolean hasVersionMicro() {
      return ((bitField0_ & 0x00000004) == 0x00000004);
    }
    /**
     * <code>required uint32 version_micro = 3;</code>
     */
    public int getVersionMicro() {
      return versionMicro_;
    }

    public static final int VERSION_STRING_FIELD_NUMBER = 4;
    private Object versionString_;
    /**
     * <code>required string version_string = 4;</code>
     */
    public boolean hasVersionString() {
      return ((bitField0_ & 0x00000008) == 0x00000008);
    }
    /**
     * <code>required string version_string = 4;</code>
     */
    public String getVersionString() {
      Object ref = versionString_;
      if (ref instanceof String) {
        return (String) ref;
      } else {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        if (bs.isValidUtf8()) {
          versionString_ = s;
        }
        return s;
      }
    }
    /**
     * <code>required string version_string = 4;</code>
     */
    public com.google.protobuf.ByteString
        getVersionStringBytes() {
      Object ref = versionString_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b =
            com.google.protobuf.ByteString.copyFromUtf8(
                (String) ref);
        versionString_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    private void initFields() {
      versionMajor_ = 0;
      versionMinor_ = 0;
      versionMicro_ = 0;
      versionString_ = "";
    }
    private byte memoizedIsInitialized = -1;
    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1) return true;
      if (isInitialized == 0) return false;

      if (!hasVersionMajor()) {
        memoizedIsInitialized = 0;
        return false;
      }
      if (!hasVersionMinor()) {
        memoizedIsInitialized = 0;
        return false;
      }
      if (!hasVersionMicro()) {
        memoizedIsInitialized = 0;
        return false;
      }
      if (!hasVersionString()) {
        memoizedIsInitialized = 0;
        return false;
      }
      memoizedIsInitialized = 1;
      return true;
    }

    public void writeTo(com.google.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
      getSerializedSize();
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        output.writeUInt32(1, versionMajor_);
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        output.writeUInt32(2, versionMinor_);
      }
      if (((bitField0_ & 0x00000004) == 0x00000004)) {
        output.writeUInt32(3, versionMicro_);
      }
      if (((bitField0_ & 0x00000008) == 0x00000008)) {
        output.writeBytes(4, getVersionStringBytes());
      }
      getUnknownFields().writeTo(output);
    }

    private int memoizedSerializedSize = -1;
    public int getSerializedSize() {
      int size = memoizedSerializedSize;
      if (size != -1) return size;

      size = 0;
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        size += com.google.protobuf.CodedOutputStream
          .computeUInt32Size(1, versionMajor_);
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        size += com.google.protobuf.CodedOutputStream
          .computeUInt32Size(2, versionMinor_);
      }
      if (((bitField0_ & 0x00000004) == 0x00000004)) {
        size += com.google.protobuf.CodedOutputStream
          .computeUInt32Size(3, versionMicro_);
      }
      if (((bitField0_ & 0x00000008) == 0x00000008)) {
        size += com.google.protobuf.CodedOutputStream
          .computeBytesSize(4, getVersionStringBytes());
      }
      size += getUnknownFields().getSerializedSize();
      memoizedSerializedSize = size;
      return size;
    }

    private static final long serialVersionUID = 0L;
    @Override
    protected Object writeReplace()
        throws java.io.ObjectStreamException {
      return super.writeReplace();
    }

    public static org.robocup_logistics.llsf_msgs.VersionProtos.VersionInfo parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static org.robocup_logistics.llsf_msgs.VersionProtos.VersionInfo parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static org.robocup_logistics.llsf_msgs.VersionProtos.VersionInfo parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static org.robocup_logistics.llsf_msgs.VersionProtos.VersionInfo parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static org.robocup_logistics.llsf_msgs.VersionProtos.VersionInfo parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return PARSER.parseFrom(input);
    }
    public static org.robocup_logistics.llsf_msgs.VersionProtos.VersionInfo parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return PARSER.parseFrom(input, extensionRegistry);
    }
    public static org.robocup_logistics.llsf_msgs.VersionProtos.VersionInfo parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      return PARSER.parseDelimitedFrom(input);
    }
    public static org.robocup_logistics.llsf_msgs.VersionProtos.VersionInfo parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return PARSER.parseDelimitedFrom(input, extensionRegistry);
    }
    public static org.robocup_logistics.llsf_msgs.VersionProtos.VersionInfo parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return PARSER.parseFrom(input);
    }
    public static org.robocup_logistics.llsf_msgs.VersionProtos.VersionInfo parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return PARSER.parseFrom(input, extensionRegistry);
    }

    public static Builder newBuilder() { return Builder.create(); }
    public Builder newBuilderForType() { return newBuilder(); }
    public static Builder newBuilder(org.robocup_logistics.llsf_msgs.VersionProtos.VersionInfo prototype) {
      return newBuilder().mergeFrom(prototype);
    }
    public Builder toBuilder() { return newBuilder(this); }

    @Override
    protected Builder newBuilderForType(
        com.google.protobuf.GeneratedMessage.BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }
    /**
     * Protobuf type {@code llsf_msgs.VersionInfo}
     */
    public static final class Builder extends
        com.google.protobuf.GeneratedMessage.Builder<Builder> implements
        // @@protoc_insertion_point(builder_implements:llsf_msgs.VersionInfo)
        org.robocup_logistics.llsf_msgs.VersionProtos.VersionInfoOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor
          getDescriptor() {
        return org.robocup_logistics.llsf_msgs.VersionProtos.internal_static_llsf_msgs_VersionInfo_descriptor;
      }

      protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
          internalGetFieldAccessorTable() {
        return org.robocup_logistics.llsf_msgs.VersionProtos.internal_static_llsf_msgs_VersionInfo_fieldAccessorTable
            .ensureFieldAccessorsInitialized(
                org.robocup_logistics.llsf_msgs.VersionProtos.VersionInfo.class, org.robocup_logistics.llsf_msgs.VersionProtos.VersionInfo.Builder.class);
      }

      // Construct using org.robocup_logistics.llsf_msgs.VersionProtos.VersionInfo.newBuilder()
      private Builder() {
        maybeForceBuilderInitialization();
      }

      private Builder(
          com.google.protobuf.GeneratedMessage.BuilderParent parent) {
        super(parent);
        maybeForceBuilderInitialization();
      }
      private void maybeForceBuilderInitialization() {
        if (com.google.protobuf.GeneratedMessage.alwaysUseFieldBuilders) {
        }
      }
      private static Builder create() {
        return new Builder();
      }

      public Builder clear() {
        super.clear();
        versionMajor_ = 0;
        bitField0_ = (bitField0_ & ~0x00000001);
        versionMinor_ = 0;
        bitField0_ = (bitField0_ & ~0x00000002);
        versionMicro_ = 0;
        bitField0_ = (bitField0_ & ~0x00000004);
        versionString_ = "";
        bitField0_ = (bitField0_ & ~0x00000008);
        return this;
      }

      public Builder clone() {
        return create().mergeFrom(buildPartial());
      }

      public com.google.protobuf.Descriptors.Descriptor
          getDescriptorForType() {
        return org.robocup_logistics.llsf_msgs.VersionProtos.internal_static_llsf_msgs_VersionInfo_descriptor;
      }

      public org.robocup_logistics.llsf_msgs.VersionProtos.VersionInfo getDefaultInstanceForType() {
        return org.robocup_logistics.llsf_msgs.VersionProtos.VersionInfo.getDefaultInstance();
      }

      public org.robocup_logistics.llsf_msgs.VersionProtos.VersionInfo build() {
        org.robocup_logistics.llsf_msgs.VersionProtos.VersionInfo result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      public org.robocup_logistics.llsf_msgs.VersionProtos.VersionInfo buildPartial() {
        org.robocup_logistics.llsf_msgs.VersionProtos.VersionInfo result = new org.robocup_logistics.llsf_msgs.VersionProtos.VersionInfo(this);
        int from_bitField0_ = bitField0_;
        int to_bitField0_ = 0;
        if (((from_bitField0_ & 0x00000001) == 0x00000001)) {
          to_bitField0_ |= 0x00000001;
        }
        result.versionMajor_ = versionMajor_;
        if (((from_bitField0_ & 0x00000002) == 0x00000002)) {
          to_bitField0_ |= 0x00000002;
        }
        result.versionMinor_ = versionMinor_;
        if (((from_bitField0_ & 0x00000004) == 0x00000004)) {
          to_bitField0_ |= 0x00000004;
        }
        result.versionMicro_ = versionMicro_;
        if (((from_bitField0_ & 0x00000008) == 0x00000008)) {
          to_bitField0_ |= 0x00000008;
        }
        result.versionString_ = versionString_;
        result.bitField0_ = to_bitField0_;
        onBuilt();
        return result;
      }

      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof org.robocup_logistics.llsf_msgs.VersionProtos.VersionInfo) {
          return mergeFrom((org.robocup_logistics.llsf_msgs.VersionProtos.VersionInfo)other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(org.robocup_logistics.llsf_msgs.VersionProtos.VersionInfo other) {
        if (other == org.robocup_logistics.llsf_msgs.VersionProtos.VersionInfo.getDefaultInstance()) return this;
        if (other.hasVersionMajor()) {
          setVersionMajor(other.getVersionMajor());
        }
        if (other.hasVersionMinor()) {
          setVersionMinor(other.getVersionMinor());
        }
        if (other.hasVersionMicro()) {
          setVersionMicro(other.getVersionMicro());
        }
        if (other.hasVersionString()) {
          bitField0_ |= 0x00000008;
          versionString_ = other.versionString_;
          onChanged();
        }
        this.mergeUnknownFields(other.getUnknownFields());
        return this;
      }

      public final boolean isInitialized() {
        if (!hasVersionMajor()) {

          return false;
        }
        if (!hasVersionMinor()) {

          return false;
        }
        if (!hasVersionMicro()) {

          return false;
        }
        if (!hasVersionString()) {

          return false;
        }
        return true;
      }

      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        org.robocup_logistics.llsf_msgs.VersionProtos.VersionInfo parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (org.robocup_logistics.llsf_msgs.VersionProtos.VersionInfo) e.getUnfinishedMessage();
          throw e;
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }
      private int bitField0_;

      private int versionMajor_ ;
      /**
       * <code>required uint32 version_major = 1;</code>
       */
      public boolean hasVersionMajor() {
        return ((bitField0_ & 0x00000001) == 0x00000001);
      }
      /**
       * <code>required uint32 version_major = 1;</code>
       */
      public int getVersionMajor() {
        return versionMajor_;
      }
      /**
       * <code>required uint32 version_major = 1;</code>
       */
      public Builder setVersionMajor(int value) {
        bitField0_ |= 0x00000001;
        versionMajor_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>required uint32 version_major = 1;</code>
       */
      public Builder clearVersionMajor() {
        bitField0_ = (bitField0_ & ~0x00000001);
        versionMajor_ = 0;
        onChanged();
        return this;
      }

      private int versionMinor_ ;
      /**
       * <code>required uint32 version_minor = 2;</code>
       */
      public boolean hasVersionMinor() {
        return ((bitField0_ & 0x00000002) == 0x00000002);
      }
      /**
       * <code>required uint32 version_minor = 2;</code>
       */
      public int getVersionMinor() {
        return versionMinor_;
      }
      /**
       * <code>required uint32 version_minor = 2;</code>
       */
      public Builder setVersionMinor(int value) {
        bitField0_ |= 0x00000002;
        versionMinor_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>required uint32 version_minor = 2;</code>
       */
      public Builder clearVersionMinor() {
        bitField0_ = (bitField0_ & ~0x00000002);
        versionMinor_ = 0;
        onChanged();
        return this;
      }

      private int versionMicro_ ;
      /**
       * <code>required uint32 version_micro = 3;</code>
       */
      public boolean hasVersionMicro() {
        return ((bitField0_ & 0x00000004) == 0x00000004);
      }
      /**
       * <code>required uint32 version_micro = 3;</code>
       */
      public int getVersionMicro() {
        return versionMicro_;
      }
      /**
       * <code>required uint32 version_micro = 3;</code>
       */
      public Builder setVersionMicro(int value) {
        bitField0_ |= 0x00000004;
        versionMicro_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>required uint32 version_micro = 3;</code>
       */
      public Builder clearVersionMicro() {
        bitField0_ = (bitField0_ & ~0x00000004);
        versionMicro_ = 0;
        onChanged();
        return this;
      }

      private Object versionString_ = "";
      /**
       * <code>required string version_string = 4;</code>
       */
      public boolean hasVersionString() {
        return ((bitField0_ & 0x00000008) == 0x00000008);
      }
      /**
       * <code>required string version_string = 4;</code>
       */
      public String getVersionString() {
        Object ref = versionString_;
        if (!(ref instanceof String)) {
          com.google.protobuf.ByteString bs =
              (com.google.protobuf.ByteString) ref;
          String s = bs.toStringUtf8();
          if (bs.isValidUtf8()) {
            versionString_ = s;
          }
          return s;
        } else {
          return (String) ref;
        }
      }
      /**
       * <code>required string version_string = 4;</code>
       */
      public com.google.protobuf.ByteString
          getVersionStringBytes() {
        Object ref = versionString_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b =
              com.google.protobuf.ByteString.copyFromUtf8(
                  (String) ref);
          versionString_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }
      /**
       * <code>required string version_string = 4;</code>
       */
      public Builder setVersionString(
          String value) {
        if (value == null) {
    throw new NullPointerException();
  }
  bitField0_ |= 0x00000008;
        versionString_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>required string version_string = 4;</code>
       */
      public Builder clearVersionString() {
        bitField0_ = (bitField0_ & ~0x00000008);
        versionString_ = getDefaultInstance().getVersionString();
        onChanged();
        return this;
      }
      /**
       * <code>required string version_string = 4;</code>
       */
      public Builder setVersionStringBytes(
          com.google.protobuf.ByteString value) {
        if (value == null) {
    throw new NullPointerException();
  }
  bitField0_ |= 0x00000008;
        versionString_ = value;
        onChanged();
        return this;
      }

      // @@protoc_insertion_point(builder_scope:llsf_msgs.VersionInfo)
    }

    static {
      defaultInstance = new VersionInfo(true);
      defaultInstance.initFields();
    }

    // @@protoc_insertion_point(class_scope:llsf_msgs.VersionInfo)
  }

  private static final com.google.protobuf.Descriptors.Descriptor
    internal_static_llsf_msgs_VersionInfo_descriptor;
  private static
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internal_static_llsf_msgs_VersionInfo_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    String[] descriptorData = {
      "\n\021VersionInfo.proto\022\tllsf_msgs\"\222\001\n\013Versi" +
      "onInfo\022\025\n\rversion_major\030\001 \002(\r\022\025\n\rversion" +
      "_minor\030\002 \002(\r\022\025\n\rversion_micro\030\003 \002(\r\022\026\n\016v" +
      "ersion_string\030\004 \002(\t\"&\n\010CompType\022\014\n\007COMP_" +
      "ID\020\320\017\022\014\n\010MSG_TYPE\020\003B0\n\037org.robocup_logis" +
      "tics.llsf_msgsB\rVersionProtos"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
        new com.google.protobuf.Descriptors.FileDescriptor.    InternalDescriptorAssigner() {
          public com.google.protobuf.ExtensionRegistry assignDescriptors(
              com.google.protobuf.Descriptors.FileDescriptor root) {
            descriptor = root;
            return null;
          }
        };
    com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        }, assigner);
    internal_static_llsf_msgs_VersionInfo_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_llsf_msgs_VersionInfo_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessage.FieldAccessorTable(
        internal_static_llsf_msgs_VersionInfo_descriptor,
        new String[] { "VersionMajor", "VersionMinor", "VersionMicro", "VersionString", });
  }

  // @@protoc_insertion_point(outer_class_scope)
}
