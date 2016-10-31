/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://ivorius.net
 */

package ivorius.reccomplex.gui.editstructure.preset;

import ivorius.ivtoolkit.tools.IvTranslations;
import ivorius.reccomplex.gui.table.*;
import ivorius.reccomplex.gui.table.cell.TableCellButton;
import ivorius.reccomplex.gui.table.cell.TableCellMultiBuilder;
import ivorius.reccomplex.gui.table.cell.TableCellPresetAction;
import ivorius.reccomplex.gui.table.cell.TableElementCell;
import ivorius.reccomplex.gui.table.datasource.TableDataSourceSegmented;
import ivorius.reccomplex.utils.PresetRegistry;
import ivorius.reccomplex.utils.presets.PresettedObject;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by lukas on 19.09.16.
 */
public class TableDataSourcePresettedObject<T> extends TableDataSourceSegmented
{
    public TableDelegate delegate;
    public TableNavigator navigator;

    public PresettedObject<T> object;
    public String saverID;

    public Runnable applyPresetAction;

    public boolean currentOnTop = false;

    public TableDataSourcePresettedObject(PresettedObject<T> object, String saverID, TableDelegate delegate, TableNavigator navigator)
    {
        this.object = object;
        this.saverID = saverID;

        this.delegate = delegate;
        this.navigator = navigator;
    }

    @Nonnull
    public static <T> TableElement getCustomizeElement(PresettedObject<T> object, String saverID, TableDelegate delegate, TableNavigator navigator, Runnable applyPresetAction)
    {
        if (!object.isCustom())
        {
            @SuppressWarnings("OptionalGetWithoutIsPresent")
            TableCellButton cell = new TableCellButton("customize", "customize", IvTranslations.format("reccomplex.preset.customize", object.presetTitle().get()), true);
            cell.addAction(() ->
            {
                object.setToCustom();
                if (applyPresetAction != null)
                    applyPresetAction.run();
                delegate.reloadData();
            });
            return new TableElementCell(cell);
        }
        else
        {
            return TableCellMultiBuilder.create(navigator, delegate)
                    .addNavigation(() -> IvTranslations.get("reccomplex.preset.save"), null,
                            () -> new TableDataSourceSavePreset<>(object, saverID, delegate, navigator)
                    ).enabled(() -> saverID != null).buildElement();
        }
    }

    @Nonnull
    public static <T> TableElement getSetElement(PresettedObject<T> object, TableDelegate delegate, List<TableCellButton> actions, Runnable applyPresetAction)
    {
        if (actions.isEmpty())
            return new TableElementCell(new TableCellButton(null, null, IvTranslations.get("reccomplex.presets"), false));

        TableCellPresetAction cell = new TableCellPresetAction("preset", actions);
        cell.addAction((actionID) ->
        {
            object.setPreset(actionID);
            if (applyPresetAction != null)
                applyPresetAction.run();
            delegate.reloadData();
        });
        if (object.getPreset() != null)
            cell.setCurrentAction(object.getPreset());
        return new TableElementCell(IvTranslations.get("reccomplex.presets"), cell);
    }

    @Nonnull
    public static <T> List<TableCellButton> getActions(PresettedObject<T> object)
    {
        PresetRegistry<T> registry = object.getPresetRegistry();
        //noinspection OptionalGetWithoutIsPresent
        return TableCellPresetAction.sorted(registry.allIDs().stream().map(type -> new TableCellButton(type, type,
                IvTranslations.format("reccomplex.preset.use", registry.title(type).orElse(type)),
                registry.description(type).orElse(null)
        ))).collect(Collectors.toList());
    }

    public TableDataSourcePresettedObject<T> withApplyPresetAction(Runnable applyPresetAction)
    {
        this.applyPresetAction = applyPresetAction;
        return this;
    }

    public TableDataSourcePresettedObject<T> withCurrentOnTop(boolean currentOnTop)
    {
        this.currentOnTop = currentOnTop;
        return this;
    }

    @Override
    public int numberOfSegments()
    {
        return 1;
    }

    @Override
    public int sizeOfSegment(int segment)
    {
        return segment == 0 ? 2 : super.sizeOfSegment(segment);
    }

    @Override
    public TableElement elementForIndexInSegment(GuiTable table, int index, int segment)
    {
        if (segment == 0)
        {
            if (index == (currentOnTop ? 1 : 0))
                return getSetElement(object, delegate, getActions(), applyPresetAction);
            else
                return getCustomizeElement(object, saverID, delegate, navigator, applyPresetAction);
        }

        return super.elementForIndexInSegment(table, index, segment);
    }

    public List<TableCellButton> getActions()
    {
        return getActions(object);
    }
}