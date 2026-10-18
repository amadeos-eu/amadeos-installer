package org.datazuul.amadeos.installer.domain;

/** Whether a whole disk is erased and partitioned, or an existing partition is formatted. */
public enum TargetMode {
    WHOLE_DISK,
    PARTITION
}
