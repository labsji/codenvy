/*
    Require setup + entry point for the application
*/

require.config({
    baseUrl : "/scripts/",
    shim: {
        'underscore' : {exports: '_' },
        'backbone' : {exports: 'Backbone', deps: ['underscore']},
        'skrollr' : {exports: 'skrollr' },
        'validation' : {deps:["jquery"]},
        'json' : {exports: 'JSON'},
        'handlebars' :  {exports: 'Handlebars'}
    },

    paths: {
        jquery: 'vendor/jquery.min',
        validation: 'vendor/jquery.validate',
        underscore: 'vendor/underscore',
        backbone: 'vendor/backbone',
        skrollr: 'vendor/skrollr',
        text : 'vendor/text',
        json : 'vendor/json2',
        handlebars : 'vendor/handlebars',
        templates: '../templates'
    }

});

require(['app'], function(Application) {
    Application.run();
});
