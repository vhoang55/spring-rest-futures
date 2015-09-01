
var simulatorApp = angular.module('simulatorApp',[]).
    controller('simulatorController',function($scope,$http){

        $scope.processErrorInjection = function () {
            $http({method : 'GET', url : 'http://localhost:8080/api/travelSuggestion'})
                .success(function(data, status) {
                    $scope.serverResponse = JSON.stringify(data);
                    $scope.processingTime = data.processingTime;
                })
                .error(function(data, status) {
                    alert("Error");
                });
        };

        $scope.processGuava7Request = function () {
            $http({method : 'GET', url : 'http://localhost:8080/api/travelGuava'})
                .success(function(data, status) {
                    $scope.guava7Response = JSON.stringify(data);
                    $scope.guava7processingTime = data.processingTime;
                })
                .error(function(data, status) {
                    alert("Error");
                });
        };

        $scope.processGuava8Request = function () {
            $http({method : 'GET', url : 'http://localhost:8080/api/travelGuavaLamda'})
                .success(function(data, status) {
                    $scope.guava8Response = JSON.stringify(data);
                    $scope.guava8processingTime = data.processingTime;
                })
                .error(function(data, status) {
                    alert("Error");
                });
        };

        $scope.processJava8Request = function () {
            $http({method : 'GET', url : 'http://localhost:8080/api/travelJava8'})
                .success(function(data, status) {
                    $scope.java8Response = JSON.stringify(data);
                    $scope.java8processingTime = data.processingTime;
                })
                .error(function(data, status) {
                    alert("Error");
                });
        };

        $scope.processRxJava8Request = function () {
            $http({method : 'GET', url : 'http://localhost:8080/api/rxJavaTravelAgent'})
                .success(function(data, status) {
                    $scope.rxJavaResponse = JSON.stringify(data);
                    $scope.rxJavaprocessingTime = data.processingTime;
                })
                .error(function(data, status) {
                    alert("Error");
                });
        };

        $scope.processAkkaRequest = function () {
            $http({method : 'GET', url : 'http://localhost:8080/api/akkaActorTravelAgent'})
                .success(function(data, status) {
                    $scope.akkaFutureResponse = JSON.stringify(data);
                    $scope.akkaFutureprocessingTime = data.processingTime;
                })
                .error(function(data, status) {
                    alert("Error");
                });
        };

    });
