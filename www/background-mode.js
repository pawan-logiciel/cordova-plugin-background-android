/*
    Copyright 2013-2017 appPlant GmbH
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at
     http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
*/

var exec    = require('cordova/exec'),
    channel = require('cordova/channel');

exports.startGettingBackgroundLocation = function(params, successCallback, errCallback)
{
	var interval = params.interval;
	var afterLastUpdateMinutes = params.after_last_update_minutes;
	var minimumDistanceChanged = params.minimum_distance_changed;
	var timeSlot = params.time_slot;

	cordova.exec(successCallback, errCallback, 'BackgroundMode', 'startGettingBackgroundLocation', [interval, afterLastUpdateMinutes, minimumDistanceChanged, timeSlot]);
};

/**
 * Deactivates the background mode. When deactivated the application
 * will not stay awake while in background.
 *
 * @return [ Void ]
 */
exports.disable = function()
{
    cordova.exec(fn, null, 'BackgroundMode', 'disable', []);
};

/**
 * Fire event with given arguments.
 *
 * @param [ String ] event The event's name.
 * @param [ Array<Object> ] The callback's arguments.
 *
 * @return [ Void ]
 */
exports.fireEvent = function (event)
{
    // var args     = Array.apply(null, arguments).slice(1),
    //     listener = this._listener[event];

    // if (!listener)
    //     return;

    // for (var i = 0; i < listener.length; i++)
    // {
    //     var fn    = listener[i][0],
    //         scope = listener[i][1];

    //     fn.apply(scope, args);
    // }
};

/**
 * Register callback for given event.
 *
 * @param [ String ] event The event's name.
 * @param [ Function ] callback The function to be exec as callback.
 * @param [ Object ] scope The callback function's scope.
 *
 * @return [ Void ]
 */
exports.on = function (event, callback, scope)
{
    // if (typeof callback !== "function")
    //     return;

    // if (!this._listener[event])
    // {
    //     this._listener[event] = [];
    // }

    // var item = [callback, scope || window];

    // this._listener[event].push(item);
};

/**
 * Unregister callback for given event.
 *
 * @param [ String ] event The event's name.
 * @param [ Function ] callback The function to be exec as callback.
 *
 * @return [ Void ]
 */
exports.un = function (event, callback)
{
    // var listener = this._listener[event];

    // if (!listener)
    //     return;

    // for (var i = 0; i < listener.length; i++)
    // {
    //     var fn = listener[i][0];

    //     if (fn == callback)
    //     {
    //         listener.splice(i, 1);
    //         break;
    //     }
    // }
};
exports._setActive = function(value)
{
    // if (this._isActive == value)
    //     return;

    // this._isActive = value;
    // this._settings = value ? this._mergeObjects({}, this._defaults) : {};
};

/**
 * @private
 *
 * Flag indicates if the mode is enabled.
 */
exports._isEnabled = false;

/**
 * @private
 *
 * Flag indicates if the mode is active.
 */
exports._isActive = false;
// Called before 'deviceready' listener will be called
channel.onCordovaReady.subscribe(function()
{
    channel.onCordovaInfoReady.subscribe(function() {
        // exports._pluginInitialize();
    });
});

// Called after 'deviceready' event
channel.deviceready.subscribe(function()
{
    // 
});