package org.springframework.util;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class FileSystemUtilsTest {

    @Test
    void deleteFile() throws Exception {
        File root = new File("../../tmp/root");
        File child = new File(root, "child");
        File grandchild = new File(child, "grandchild");

        grandchild.mkdirs();

        File bar = new File(child, "bar.md");
        bar.createNewFile();

        assertThat(root.exists()).isTrue();
        assertThat(child.exists()).isTrue();
        assertThat(grandchild.exists()).isTrue();
        assertThat(bar.exists()).isTrue();

        FileSystemUtils.deleteRecursively(root);

        assertThat(root.exists()).isFalse();
        assertThat(child.exists()).isFalse();
        assertThat(grandchild.exists()).isFalse();
        assertThat(bar.exists()).isFalse();
    }

    @Test
    void copyRecursively() throws Exception {
        File src = new File("../../tmp/src");
        File child = new File(src, "child");
        File grandchild = new File(child, "grandchild");

        grandchild.mkdirs();

        File bar = new File(child, "bar.md");
        bar.createNewFile();

        assertThat(src.exists()).isTrue();
        assertThat(child.exists()).isTrue();
        assertThat(grandchild.exists()).isTrue();
        assertThat(bar.exists()).isTrue();

        File dest = new File("../../dest");
        FileSystemUtils.copyRecursively(src, dest);

        assertThat(dest.exists()).isTrue();
        assertThat(new File(dest, child.getName()).exists()).isTrue();

        FileSystemUtils.deleteRecursively(src);
        assertThat(src.exists()).isFalse();
    }

}
