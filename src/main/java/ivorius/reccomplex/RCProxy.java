/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex;

import java.io.File;

/**
 * Created by lukas on 24.05.14.
 */
public interface RCProxy
{
    File getDataDirectory();

    void loadConfig(String configID);

    void registerRenderers();
}
