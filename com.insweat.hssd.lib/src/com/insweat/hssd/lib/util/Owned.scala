package com.insweat.hssd.lib.util

trait Owned[T] {
    def owner: T

    def checkOwnership(owner: T) {
        if(owner != Owned.this.owner) {
            throw new IllegalArgumentException(
                    s"$this is owned by ${this.owner}, not $owner");
        }
    }
}
