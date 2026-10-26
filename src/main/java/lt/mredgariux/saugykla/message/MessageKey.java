package lt.mredgariux.saugykla.message;

import lt.mredgariux.messages.language.LanguageKey;

/** Translation keys owned by Saugykla; text lives in langs/en.yml. */
public enum MessageKey implements LanguageKey {
    CONSOLE("command.console"),
    UNKNOWN_ARGUMENTS("command.unknown-arguments"),
    NO_REGIONS("storage.no-regions"),
    STORAGE_TOO_FAR("storage.too-far"),
    RESET_NO_PERMISSION("reset.no-permission"),
    RESET_SUCCESS("reset.success"),
    RESET_FAILED("reset.failed"),
    UNKNOWN_ITEM("item.unknown"),
    HIGHLIGHT_HOLD_ITEM("highlight.hold-item"),
    HIGHLIGHT_USAGE("highlight.usage"),
    ITEM_NOT_STORED("item.not-stored"),
    HIGHLIGHT_TOO_FAR("highlight.too-far"),
    REGION_NO_PERMISSION("region.no-permission"),
    REGION_USAGE("region.usage"),
    REGION_LOOK_AT_BLOCK("region.look-at-block"),
    REGION_START_SELECTED("region.start-selected"),
    REGION_START_REQUIRED("region.start-required"),
    REGION_WORLD_MISMATCH("region.world-mismatch"),
    REGION_TOO_LARGE("region.too-large"),
    REGION_TOO_SMALL("region.too-small"),
    REGION_CREATED("region.created"),
    REGION_CREATE_FAILED("region.create-failed"),
    SEARCH_HOLD_ITEM("search.hold-item"),
    SEARCH_FOUND("search.found"),
    SEARCH_NOT_FOUND("search.not-found"),
    DEBUG_NO_PERMISSION("debug.no-permission"),
    DEBUG_REGIONS("debug.regions"),
    DEBUG_REGION("debug.region"),
    DEBUG_BARRELS("debug.barrels"),
    DEBUG_BARREL("debug.barrel"),
    DEBUG_HIGHLIGHTS("debug.highlights"),
    DEBUG_HIGHLIGHT("debug.highlight"),
    RESOURCE_NOT_ALLOWED("resources.not-allowed"),
    RESOURCES_SAVED("resources.saved"),
    HIGHLIGHT_HIDDEN("highlight.hidden"),
    HIGHLIGHT_SHOWN("highlight.shown"),
    STORAGE_VERTICAL_FULL("storage.vertical-full"),
    STORAGE_HORIZONTAL_FULL("storage.horizontal-full"),
    PLACEMENT_BLOCKED("storage.placement-blocked"),
    BARRELS_REQUIRED("resources.barrels-required"),
    SIGNS_REQUIRED("resources.signs-required"),
    BARREL_CREATE_FAILED("storage.create-failed"),
    HIGHLIGHT_EXPIRED("highlight.expired"),
    HIGHLIGHT_BARREL_REMOVED("highlight.barrel-removed"),
    BARREL_WRONG_ITEM("barrel.wrong-item"),
    BARREL_EMPTY_REMOVED("barrel.empty-removed"),
    BARREL_REMOVE_FAILED("barrel.remove-failed"),
    BARREL_REMOVED("barrel.removed");

    private final String path;

    MessageKey(String path) {
        this.path = path;
    }

    @Override
    public String key() {
        return path;
    }
}
