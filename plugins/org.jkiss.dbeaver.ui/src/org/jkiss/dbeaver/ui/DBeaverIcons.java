/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

package org.jkiss.dbeaver.ui;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBIconComposite;
import org.jkiss.dbeaver.model.DBPImage;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * DBeaverIcons
 */
public class DBeaverIcons
{
    private static final Log log = Log.getLog(DBeaverIcons.class);

    private static final boolean useLegacyOverlay;

    static {
        boolean hasCachedImageDataProvider;
        try {
            Class.forName("org.eclipse.jface.resource.CompositeImageDescriptor$CachedImageDataProvider");
            hasCachedImageDataProvider = true;
        } catch (ClassNotFoundException e) {
            hasCachedImageDataProvider = false;
        }
        useLegacyOverlay = !hasCachedImageDataProvider;
    }

    private static class IconDescriptor {
        @NotNull
        String id;
        @NotNull
        Image image;
        @NotNull
        ImageDescriptor imageDescriptor;

        IconDescriptor(@NotNull String id, @NotNull ImageDescriptor imageDescriptor) {
            this.id = id;
            this.image = imageDescriptor.createImage(false);
            this.imageDescriptor = imageDescriptor;
        }

        IconDescriptor(@NotNull String id, @NotNull Image image) {
            this.id = id;
            this.image = image;
            this.imageDescriptor = ImageDescriptor.createFromImage(image);
        }

        IconDescriptor(@NotNull String id, @NotNull Image image, @NotNull ImageDescriptor imageDescriptor) {
            this.id = id;
            this.image = image;
            this.imageDescriptor = imageDescriptor;
        }
    }

    private static Map<String, IconDescriptor> imageMap = new HashMap<>();
    private static Map<String, IconDescriptor> compositeMap = new HashMap<>();
    private static Image viewMenuImage;
    private static ImageDescriptor viewMenuImageDescriptor;

    @NotNull
    public static Image getImage(@NotNull DBPImage image) {
        return getIconDescriptor(image).image;
    }

    @Nullable
    public static Image getImageByLocation(@NotNull String iconLocation) {
        IconDescriptor icon = getIconByLocation(iconLocation);
        if (icon == null) {
            return null;
        }
        return icon.image;
    }

    @NotNull
    public static ImageDescriptor getImageDescriptor(@NotNull DBPImage image) {
        return getIconDescriptor(image).imageDescriptor; 
    }
    
    private static IconDescriptor getIconDescriptor(DBPImage image) {
        if (image == null) {
            return getIconDescriptor(DBIcon.TYPE_UNKNOWN);
        } else if (image instanceof DBIconBinary) {
            return new IconDescriptor(
                "[" + image.getLocation() + "]",
                ((DBIconBinary) image).getImage(),
                ((DBIconBinary) image).getImageDescriptor()
            );
        } else if (image instanceof DBIconComposite) {
            IconDescriptor icon = getIconDescriptor(((DBIconComposite) image).getMain());
            return getCompositeIcon(icon, (DBIconComposite) image);
        } else if (image instanceof DBIcon) {
            IconDescriptor icon = getIconByLocation(image.getLocation());
            if (icon == null) {
                log.error("Image '" + image.getLocation() + "' not found");
                return getIconDescriptor(DBIcon.TYPE_UNKNOWN);
            } else {
                return icon;
            }
        } else {
            log.error("Unexpected image of type " + image.getClass());
            return getIconDescriptor(DBIcon.TYPE_UNKNOWN);
        }
    }

    private static IconDescriptor getCompositeIcon(IconDescriptor mainIcon, DBIconComposite image) {
        if (!image.hasOverlays()) {
            return mainIcon;
        }
        String compositeId = mainIcon.id + "^" +
            (image.getTopLeft() == null ? "" : image.getTopLeft().getLocation()) + "^" +
            (image.getTopRight() == null ? "" : image.getTopRight().getLocation()) + "^" +
            (image.getBottomLeft() == null ? "" : image.getBottomLeft().getLocation()) + "^" +
            (image.getBottomRight() == null ? "" : image.getBottomRight().getLocation());
        IconDescriptor icon = compositeMap.get(compositeId);
        if (icon == null) {
            Image resultImage;
            if (useLegacyOverlay) {
                OverlayImageDescriptorLegacy ovrImage = new OverlayImageDescriptorLegacy(mainIcon.image.getImageData());
                if (image.getTopLeft() != null)
                    ovrImage.setTopLeft(accumulateDecorations(image, i -> i.getTopLeft()));
                if (image.getTopRight() != null)
                    ovrImage.setTopRight(accumulateDecorations(image, i -> i.getTopRight()));
                if (image.getBottomLeft() != null)
                    ovrImage.setBottomLeft(accumulateDecorations(image, i -> i.getBottomLeft()));
                if (image.getBottomRight() != null)
                    ovrImage.setBottomRight(accumulateDecorations(image, i -> i.getBottomRight()));
                resultImage = ovrImage.createImage();
            } else {
                OverlayImageDescriptor ovrImage = new OverlayImageDescriptor(mainIcon.imageDescriptor);
                if (image.getTopLeft() != null)
                    ovrImage.setTopLeft(accumulateDecorations(image, i -> i.getTopLeft()));
                if (image.getTopRight() != null)
                    ovrImage.setTopRight(accumulateDecorations(image, i -> i.getTopRight()));
                if (image.getBottomLeft() != null)
                    ovrImage.setBottomLeft(accumulateDecorations(image, i -> i.getBottomLeft()));
                if (image.getBottomRight() != null)
                    ovrImage.setBottomRight(accumulateDecorations(image, i -> i.getBottomRight()));
                resultImage = ovrImage.createImage();
            }
            icon = new IconDescriptor(compositeId, resultImage);
            compositeMap.put(compositeId, icon);
        }
        return icon;
    }

    @NotNull
    private static ImageDescriptor[] accumulateDecorations(
        @NotNull DBIconComposite image,
        @NotNull Function<DBIconComposite, DBPImage> map
    ) {
        DBPImage base = image.getMain();
        if (base instanceof DBIconComposite) {
            List<ImageDescriptor> decorations = new ArrayList<>();
            decorations.add(getImageDescriptor(map.apply(image)));
            do {
                image = (DBIconComposite) base;
                decorations.add(getImageDescriptor(map.apply(image)));
                base = image.getMain();
            } while (base instanceof DBIconComposite);
            return decorations.toArray(ImageDescriptor[]::new);
        } else {
            return new ImageDescriptor[]{getImageDescriptor(map.apply(image))};
        }
    }

    private static IconDescriptor getIconByLocation(String location) {
        IconDescriptor icon = imageMap.get(location);
        if (icon == null) {
            try {
                ImageDescriptor imageDescriptor = ImageDescriptor.createFromURL(new URL(location));
                icon = new IconDescriptor(location, imageDescriptor);
                if (icon.image == null) {
                    log.warn("Bad image: " + location);
                    return null;
                } else {
                    imageMap.put(location, icon);
                }
            } catch (Exception e) {
                log.error(e);
                return null;
            }
        }
        return icon;
    }

    public static synchronized Image getViewMenuImage() {
        if (viewMenuImage == null) {
            Display d = Display.getCurrent();

            Image viewMenu = new Image(d, 16, 16);
            Image viewMenuMask = new Image(d, 16, 16);

            Display display = Display.getCurrent();
            GC gc = new GC(viewMenu);
            GC maskgc = new GC(viewMenuMask);
            gc.setForeground(display
                .getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));
            gc.setBackground(display.getSystemColor(SWT.COLOR_LIST_BACKGROUND));

            int[] shapeArray = new int[]{6, 3, 15, 3, 11, 7, 10, 7};
            gc.fillPolygon(shapeArray);
            gc.drawPolygon(shapeArray);

            Color black = display.getSystemColor(SWT.COLOR_BLACK);
            Color white = display.getSystemColor(SWT.COLOR_WHITE);

            maskgc.setBackground(black);
            maskgc.fillRectangle(0, 0, 16, 16);

            maskgc.setBackground(white);
            maskgc.setForeground(white);
            maskgc.fillPolygon(shapeArray);
            maskgc.drawPolygon(shapeArray);
            gc.dispose();
            maskgc.dispose();

            ImageData data = viewMenu.getImageData();
            data.transparentPixel = data.getPixel(0, 0);

            viewMenuImage = new Image(d, viewMenu.getImageData(),
                viewMenuMask.getImageData());
            viewMenu.dispose();
            viewMenuMask.dispose();
        }
        return viewMenuImage;
    }

    public static synchronized ImageDescriptor getViewMenuImageDescriptor() {
        if (viewMenuImageDescriptor == null) {
            viewMenuImageDescriptor = ImageDescriptor.createFromImage(
                    getViewMenuImage());
        }
        return viewMenuImageDescriptor;
    }
}
