package net.earthcomputer.descentintodarkness.generator;

import net.minecraft.network.chat.Component;

public interface CaveGenProgressListener {
    CaveGenProgressListener EMPTY = new CaveGenProgressListener() {
        @Override
        public void pushProgress(Component name) {
        }

        @Override
        public void popProgress() {
        }

        @Override
        public void setProgress(float progress) {
        }

        @Override
        public void finish() {
        }
    };

    void pushProgress(Component name);

    void popProgress();

    void setProgress(float progress);

    void finish();
}
