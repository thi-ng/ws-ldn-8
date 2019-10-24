var CLOSURE_BASE_PATH = "out/goog/";
var CLOSURE_UNCOMPILED_DEFINES = {"cljs.core._STAR_target_STAR_":"webworker"};
var CLOSURE_IMPORT_SCRIPT = (function(global) { return function(src) {global['importScripts'](src); return true;};})(this);
if(typeof goog == 'undefined') importScripts("out/goog/base.js");
importScripts("out/cljs_deps.js");
goog.require("process.env");
goog.require("ex05.worker");
