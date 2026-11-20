import json
import os

collection = {
    "info": {
        "_postman_id": "eiu-pdds-e2e-suite",
        "name": "EIU PDDS Comprehensive 21-API Verification Suite",
        "description": "Full end-to-end regression test suite covering all 21 REST API endpoints of EIU Plant Disease Detection System.",
        "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
    },
    "variable": [
        {"key": "baseUrl", "value": "http://localhost:8080", "type": "string"},
        {"key": "accessToken", "value": "", "type": "string"},
        {"key": "refreshToken", "value": "", "type": "string"},
        {"key": "adminAccessToken", "value": "", "type": "string"},
        {"key": "currentUsername", "value": "", "type": "string"},
        {"key": "currentUserId", "value": "", "type": "string"},
        {"key": "adminUserId", "value": "", "type": "string"},
        {"key": "sampleDiseaseId", "value": "", "type": "string"},
        {"key": "createdDiseaseId", "value": "", "type": "string"},
        {"key": "createdTreatmentId", "value": "", "type": "string"},
        {"key": "createdDiagnosisId", "value": "", "type": "string"},
        {"key": "userDiagId", "value": "", "type": "string"},
        {"key": "userDiagId2", "value": "", "type": "string"},
        {"key": "guestDiagId1", "value": "", "type": "string"},
        {"key": "reuseTestRefreshToken", "value": "", "type": "string"},
        {"key": "rotatedRefreshToken", "value": "", "type": "string"}
    ],
    "item": [
        # =====================================================================
        # 1. PLANT CATALOG
        # =====================================================================
        {
            "name": "1. Plant Catalog",
            "item": [
                {
                    "name": "1.1 GET /api/v1/plants - Get All Plants",
                    "request": {
                        "method": "GET",
                        "header": [{"key": "Accept", "value": "application/json"}],
                        "url": {"raw": "{{baseUrl}}/api/v1/plants", "host": ["{{baseUrl}}"], "path": ["api", "v1", "plants"]}
                    },
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Status code is 200 OK', function () { pm.response.to.have.status(200); });",
                                "var res = pm.response.json();",
                                "pm.test('Response indicates success', function () { pm.expect(res.success).to.be.true; });",
                                "pm.test('Plants list contains all 14 crops', function () {",
                                "    pm.expect(res.data).to.be.an('array');",
                                "    pm.expect(res.data.length).to.be.at.least(13);",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }]
                },
                {
                    "name": "1.2 GET /api/v1/plants/{plantCode} - Get Plant by Code",
                    "request": {
                        "method": "GET",
                        "header": [{"key": "Accept", "value": "application/json"}],
                        "url": {"raw": "{{baseUrl}}/api/v1/plants/TOMATO", "host": ["{{baseUrl}}"], "path": ["api", "v1", "plants", "TOMATO"]}
                    },
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Status code is 200 OK', function () { pm.response.to.have.status(200); });",
                                "var res = pm.response.json();",
                                "pm.test('Plant details match requested code', function () {",
                                "    pm.expect(res.success).to.be.true;",
                                "    pm.expect(res.data.plantCode).to.eql('TOMATO');",
                                "    pm.expect(res.data.commonName).to.eql('Tomato');",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }]
                }
            ]
        },

        # =====================================================================
        # 2. AUTHENTICATION & IDENTITY LIFECYCLE
        # =====================================================================
        {
            "name": "2. Authentication & Identity",
            "item": [
                {
                    "name": "2.1 POST /api/v1/auth/register - Register New User",
                    "event": [{
                        "listen": "prerequest",
                        "script": {
                            "exec": [
                                "var ts = Date.now();",
                                "var username = 'user_' + ts;",
                                "var email = 'user_' + ts + '@eiu.edu.vn';",
                                "pm.collectionVariables.set('currentUsername', username);",
                                "pm.collectionVariables.set('currentEmail', email);",
                                "pm.collectionVariables.set('currentPassword', 'TestPass123!');"
                            ],
                            "type": "text/javascript"
                        }
                    }, {
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Status code is 200 OK or 201 Created', function () {",
                                "    pm.expect([200, 201]).to.include(pm.response.code);",
                                "});",
                                "var res = pm.response.json();",
                                "pm.test('Tokens generated upon registration', function () {",
                                "    pm.expect(res.success).to.be.true;",
                                "    pm.expect(res.data.accessToken).to.be.a('string');",
                                "    pm.expect(res.data.refreshToken).to.be.a('string');",
                                "    pm.collectionVariables.set('accessToken', res.data.accessToken);",
                                "    pm.collectionVariables.set('refreshToken', res.data.refreshToken);",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "POST",
                        "header": [{"key": "Content-Type", "value": "application/json"}],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n    \"username\": \"{{currentUsername}}\",\n    \"email\": \"{{currentEmail}}\",\n    \"password\": \"{{currentPassword}}\",\n    \"fullName\": \"Postman Test Runner\",\n    \"phoneNumber\": \"+84988776655\"\n}"
                        },
                        "url": {"raw": "{{baseUrl}}/api/v1/auth/register", "host": ["{{baseUrl}}"], "path": ["api", "v1", "auth", "register"]}
                    }
                },
                {
                    "name": "2.2 POST /api/v1/auth/login - Authenticate User Credentials",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Status code is 200 OK', function () { pm.response.to.have.status(200); });",
                                "var res = pm.response.json();",
                                "pm.test('Login returns valid tokens and profile', function () {",
                                "    pm.expect(res.success).to.be.true;",
                                "    pm.expect(res.data.accessToken).to.be.a('string');",
                                "    pm.expect(res.data.refreshToken).to.be.a('string');",
                                "    pm.expect(res.data.user.username).to.eql(pm.collectionVariables.get('currentUsername'));",
                                "    pm.collectionVariables.set('accessToken', res.data.accessToken);",
                                "    pm.collectionVariables.set('refreshToken', res.data.refreshToken);",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "POST",
                        "header": [{"key": "Content-Type", "value": "application/json"}],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n    \"emailOrUsername\": \"{{currentUsername}}\",\n    \"password\": \"{{currentPassword}}\",\n    \"deviceInfo\": \"Postman CLI Test Engine\"\n}"
                        },
                        "url": {"raw": "{{baseUrl}}/api/v1/auth/login", "host": ["{{baseUrl}}"], "path": ["api", "v1", "auth", "login"]}
                    }
                },
                {
                    "name": "2.3 GET /api/v1/auth/me - Retrieve Current User Profile",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Status code is 200 OK', function () { pm.response.to.have.status(200); });",
                                "var res = pm.response.json();",
                                "pm.test('User identity matches token subject', function () {",
                                "    pm.expect(res.success).to.be.true;",
                                "    pm.expect(res.data.username).to.eql(pm.collectionVariables.get('currentUsername'));",
                                "    pm.expect(res.data.role).to.eql('ROLE_USER');",
                                "    pm.collectionVariables.set('currentUserId', res.data.id);",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "GET",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{accessToken}}"},
                            {"key": "Accept", "value": "application/json"}
                        ],
                        "url": {"raw": "{{baseUrl}}/api/v1/auth/me", "host": ["{{baseUrl}}"], "path": ["api", "v1", "auth", "me"]}
                    }
                },
                {
                    "name": "2.4 POST /api/v1/auth/refresh - Refresh Access Token",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Status code is 200 OK', function () { pm.response.to.have.status(200); });",
                                "var res = pm.response.json();",
                                "pm.test('New access token successfully issued', function () {",
                                "    pm.expect(res.success).to.be.true;",
                                "    pm.expect(res.data.accessToken).to.be.a('string');",
                                "    pm.collectionVariables.set('accessToken', res.data.accessToken);",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "POST",
                        "header": [{"key": "Content-Type", "value": "application/json"}],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n    \"refreshToken\": \"{{refreshToken}}\"\n}"
                        },
                        "url": {"raw": "{{baseUrl}}/api/v1/auth/refresh", "host": ["{{baseUrl}}"], "path": ["api", "v1", "auth", "refresh"]}
                    }
                },
                {
                    "name": "2.5 POST /api/v1/auth/google - Google OAuth2 Signature Rejection Guard",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Invalid Google token is rejected with 401 or 400', function () {",
                                "    pm.expect([400, 401]).to.include(pm.response.code);",
                                "    var res = pm.response.json();",
                                "    pm.expect(res.success).to.be.false;",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "POST",
                        "header": [{"key": "Content-Type", "value": "application/json"}],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n    \"idToken\": \"invalid_mock_google_id_token_for_security_test\",\n    \"deviceInfo\": \"Postman Runner\"\n}"
                        },
                        "url": {"raw": "{{baseUrl}}/api/v1/auth/google", "host": ["{{baseUrl}}"], "path": ["api", "v1", "auth", "google"]}
                    }
                },
                {
                    "name": "2.6 POST /api/v1/auth/logout - Logout & Invalidate Session",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Status code is 200 OK', function () { pm.response.to.have.status(200); });",
                                "var res = pm.response.json();",
                                "pm.test('Session invalidated successfully', function () {",
                                "    pm.expect(res.success).to.be.true;",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "POST",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{accessToken}}"},
                            {"key": "Content-Type", "value": "application/json"}
                        ],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n    \"refreshToken\": \"{{refreshToken}}\"\n}"
                        },
                        "url": {"raw": "{{baseUrl}}/api/v1/auth/logout", "host": ["{{baseUrl}}"], "path": ["api", "v1", "auth", "logout"]}
                    }
                }
            ]
        },

        # =====================================================================
        # 3. DISEASE CATALOG & ADMIN MANAGEMENT
        # =====================================================================
        {
            "name": "3. Disease Catalog & Management",
            "item": [
                {
                    "name": "3.0 Admin Login Helper",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Admin login successful (200 OK)', function () { pm.response.to.have.status(200); });",
                                "var res = pm.response.json();",
                                "pm.collectionVariables.set('adminAccessToken', res.data.accessToken);",
                                "pm.collectionVariables.set('adminUserId', res.data.user.id);"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "POST",
                        "header": [{"key": "Content-Type", "value": "application/json"}],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n    \"emailOrUsername\": \"admin_eiu\",\n    \"password\": \"AdminPassword123!\"\n}"
                        },
                        "url": {"raw": "{{baseUrl}}/api/v1/auth/login", "host": ["{{baseUrl}}"], "path": ["api", "v1", "auth", "login"]}
                    }
                },
                {
                    "name": "3.1 GET /api/v1/diseases - Get Paginated Diseases",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Status code is 200 OK', function () { pm.response.to.have.status(200); });",
                                "var res = pm.response.json();",
                                "pm.test('Pagination structure is valid', function () {",
                                "    pm.expect(res.success).to.be.true;",
                                "    pm.expect(res.data.content).to.be.an('array');",
                                "    pm.expect(res.data.totalElements).to.be.at.least(30);",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "GET",
                        "header": [{"key": "Accept", "value": "application/json"}],
                        "url": {
                            "raw": "{{baseUrl}}/api/v1/diseases?page=0&size=20",
                            "host": ["{{baseUrl}}"],
                            "path": ["api", "v1", "diseases"],
                            "query": [
                                {"key": "page", "value": "0"},
                                {"key": "size", "value": "20"}
                            ]
                        }
                    }
                },
                {
                    "name": "3.2 GET /api/v1/diseases/{idOrCode} - Get Disease by Code",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Status code is 200 OK', function () { pm.response.to.have.status(200); });",
                                "var res = pm.response.json();",
                                "pm.test('Disease details retrieved correctly', function () {",
                                "    pm.expect(res.success).to.be.true;",
                                "    pm.expect(res.data.diseaseCode).to.eql('TOMATO_EARLY_BLIGHT');",
                                "    pm.collectionVariables.set('sampleDiseaseId', res.data.id);",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "GET",
                        "header": [{"key": "Accept", "value": "application/json"}],
                        "url": {"raw": "{{baseUrl}}/api/v1/diseases/TOMATO_EARLY_BLIGHT", "host": ["{{baseUrl}}"], "path": ["api", "v1", "diseases", "TOMATO_EARLY_BLIGHT"]}
                    }
                },
                {
                    "name": "3.3 POST /api/v1/diseases - Create Disease Catalog Record (Admin)",
                    "event": [{
                        "listen": "prerequest",
                        "script": {
                            "exec": [
                                "var dCode = 'TEST_DISEASE_' + Date.now();",
                                "pm.collectionVariables.set('tempDiseaseCode', dCode);"
                            ],
                            "type": "text/javascript"
                        }
                    }, {
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Disease created with status 201 Created or 200 OK', function () {",
                                "    pm.expect([200, 201]).to.include(pm.response.code);",
                                "});",
                                "var res = pm.response.json();",
                                "pm.test('Disease record created with valid ID', function () {",
                                "    pm.expect(res.success).to.be.true;",
                                "    pm.expect(res.data.id).to.be.a('string');",
                                "    pm.collectionVariables.set('createdDiseaseId', res.data.id);",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "POST",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{adminAccessToken}}"},
                            {"key": "Content-Type", "value": "application/json"}
                        ],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n    \"diseaseCode\": \"{{tempDiseaseCode}}\",\n    \"commonName\": \"Automated Postman Test Blight\",\n    \"scientificName\": \"Testus pathologicus\",\n    \"severity\": \"MODERATE\",\n    \"symptoms\": \"Simulated foliar yellow spots generated by Postman runner\",\n    \"description\": \"Transient test disease record for regression validation\",\n    \"plantCodes\": [\"TOMATO\"]\n}"
                        },
                        "url": {"raw": "{{baseUrl}}/api/v1/diseases", "host": ["{{baseUrl}}"], "path": ["api", "v1", "diseases"]}
                    }
                },
                {
                    "name": "3.4 PUT /api/v1/diseases/{id} - Update Disease Catalog Record (Admin)",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Disease updated with status 200 OK', function () { pm.response.to.have.status(200); });",
                                "var res = pm.response.json();",
                                "pm.test('Updated common name reflected in response', function () {",
                                "    pm.expect(res.success).to.be.true;",
                                "    pm.expect(res.data.commonName).to.eql('Automated Postman Test Blight - Updated');",
                                "    pm.expect(res.data.severity).to.eql('HIGH');",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "PUT",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{adminAccessToken}}"},
                            {"key": "Content-Type", "value": "application/json"}
                        ],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n    \"commonName\": \"Automated Postman Test Blight - Updated\",\n    \"scientificName\": \"Testus pathologicus var. updated\",\n    \"severity\": \"HIGH\",\n    \"symptoms\": \"Updated severe leaf necroses\",\n    \"description\": \"Updated description text\",\n    \"plantCodes\": [\"TOMATO\", \"POTATO\"]\n}"
                        },
                        "url": {"raw": "{{baseUrl}}/api/v1/diseases/{{createdDiseaseId}}", "host": ["{{baseUrl}}"], "path": ["api", "v1", "diseases", "{{createdDiseaseId}}"]}
                    }
                },
                {
                    "name": "3.5 DELETE /api/v1/diseases/{id} - Delete Disease Catalog Record (Admin)",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Disease deleted with status 200 OK', function () { pm.response.to.have.status(200); });",
                                "var res = pm.response.json();",
                                "pm.test('Soft delete successful', function () {",
                                "    pm.expect(res.success).to.be.true;",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "DELETE",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{adminAccessToken}}"},
                            {"key": "Accept", "value": "application/json"}
                        ],
                        "url": {"raw": "{{baseUrl}}/api/v1/diseases/{{createdDiseaseId}}", "host": ["{{baseUrl}}"], "path": ["api", "v1", "diseases", "{{createdDiseaseId}}"]}
                    }
                }
            ]
        },

        # =====================================================================
        # 4. TREATMENT PROTOCOLS MANAGEMENT
        # =====================================================================
        {
            "name": "4. Treatment Protocols",
            "item": [
                {
                    "name": "4.1 GET /api/v1/diseases/{diseaseId}/treatments - Get Treatments for Disease",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Status code is 200 OK', function () { pm.response.to.have.status(200); });",
                                "var res = pm.response.json();",
                                "pm.test('Treatments list returned for disease', function () {",
                                "    pm.expect(res.success).to.be.true;",
                                "    pm.expect(res.data).to.be.an('array');",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "GET",
                        "header": [{"key": "Accept", "value": "application/json"}],
                        "url": {"raw": "{{baseUrl}}/api/v1/diseases/{{sampleDiseaseId}}/treatments", "host": ["{{baseUrl}}"], "path": ["api", "v1", "diseases", "{{sampleDiseaseId}}", "treatments"]}
                    }
                },
                {
                    "name": "4.2 POST /api/v1/diseases/{diseaseId}/treatments - Create Treatment (Admin)",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Treatment created with status 201 Created or 200 OK', function () {",
                                "    pm.expect([200, 201]).to.include(pm.response.code);",
                                "});",
                                "var res = pm.response.json();",
                                "pm.test('Treatment record created with valid ID', function () {",
                                "    pm.expect(res.success).to.be.true;",
                                "    pm.expect(res.data.id).to.be.a('string');",
                                "    pm.collectionVariables.set('createdTreatmentId', res.data.id);",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "POST",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{adminAccessToken}}"},
                            {"key": "Content-Type", "value": "application/json"}
                        ],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n    \"treatmentType\": \"BIOLOGICAL\",\n    \"title\": \"Postman Automated Bio-spray Protocol\",\n    \"instructions\": \"Spray bio-extract every 7 days as a preventive barrier\",\n    \"activeIngredients\": \"Bacillus subtilis 10^9 CFU/g\",\n    \"dosageRecommendation\": \"5g per 1L water\",\n    \"safetyIntervalDays\": 0,\n    \"warning\": \"Do not mix with chemical bactericides\"\n}"
                        },
                        "url": {"raw": "{{baseUrl}}/api/v1/diseases/{{sampleDiseaseId}}/treatments", "host": ["{{baseUrl}}"], "path": ["api", "v1", "diseases", "{{sampleDiseaseId}}", "treatments"]}
                    }
                },
                {
                    "name": "4.3 PUT /api/v1/treatments/{treatmentId} - Update Treatment (Admin)",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Treatment updated with status 200 OK', function () { pm.response.to.have.status(200); });",
                                "var res = pm.response.json();",
                                "pm.test('Updated treatment fields reflected in response', function () {",
                                "    pm.expect(res.success).to.be.true;",
                                "    pm.expect(res.data.title).to.eql('Postman Automated Bio-spray Protocol - Updated');",
                                "    pm.expect(res.data.safetyIntervalDays).to.eql(1);",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "PUT",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{adminAccessToken}}"},
                            {"key": "Content-Type", "value": "application/json"}
                        ],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n    \"treatmentType\": \"BIOLOGICAL\",\n    \"title\": \"Postman Automated Bio-spray Protocol - Updated\",\n    \"instructions\": \"Apply in early morning for maximum spore efficacy\",\n    \"activeIngredients\": \"Bacillus subtilis 10^9 CFU/g + Trichoderma\",\n    \"dosageRecommendation\": \"10g per 1L water\",\n    \"safetyIntervalDays\": 1,\n    \"warning\": \"Store in a cool dry place\"\n}"
                        },
                        "url": {"raw": "{{baseUrl}}/api/v1/treatments/{{createdTreatmentId}}", "host": ["{{baseUrl}}"], "path": ["api", "v1", "treatments", "{{createdTreatmentId}}"]}
                    }
                },
                {
                    "name": "4.4 DELETE /api/v1/treatments/{treatmentId} - Delete Treatment (Admin)",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Treatment deleted with status 200 OK', function () { pm.response.to.have.status(200); });",
                                "var res = pm.response.json();",
                                "pm.test('Soft delete successful', function () {",
                                "    pm.expect(res.success).to.be.true;",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "DELETE",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{adminAccessToken}}"},
                            {"key": "Accept", "value": "application/json"}
                        ],
                        "url": {"raw": "{{baseUrl}}/api/v1/treatments/{{createdTreatmentId}}", "host": ["{{baseUrl}}"], "path": ["api", "v1", "treatments", "{{createdTreatmentId}}"]}
                    }
                }
            ]
        },

        # =====================================================================
        # 5. DIAGNOSIS & AI INFERENCE
        # =====================================================================
        {
            "name": "5. Diagnosis & AI Pipeline",
            "item": [
                {
                    "name": "5.1 POST /api/v1/diagnoses/analyze - Multipart Leaf Image Inference",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Analysis completed with status 200 OK', function () { pm.response.to.have.status(200); });",
                                "var res = pm.response.json();",
                                "var diagId = res.data.diagnosisId || res.data.id;",
                                "pm.test('Analysis response includes diagnosis ID and detections', function () {",
                                "    pm.expect(res.success).to.be.true;",
                                "    pm.expect(diagId).to.be.a('string');",
                                "    pm.expect(res.data.originalImageUrl || res.data.imageUrl).to.be.a('string');",
                                "    pm.expect(res.data.boundingBoxes).to.be.an('array');",
                                "    pm.collectionVariables.set('createdDiagnosisId', diagId);",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "POST",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{adminAccessToken}}"}
                        ],
                        "body": {
                            "mode": "formdata",
                            "formdata": [
                                {"key": "image", "type": "file", "src": "postman/test_leaf.jpg"},
                                {"key": "plantCode", "value": "TOMATO", "type": "text"},
                                {"key": "latitude", "value": "11.0528", "type": "text"},
                                {"key": "longitude", "value": "106.6661", "type": "text"},
                                {"key": "deviceInfo", "value": "Postman CLI Automated Test", "type": "text"}
                            ]
                        },
                        "url": {"raw": "{{baseUrl}}/api/v1/diagnoses/analyze", "host": ["{{baseUrl}}"], "path": ["api", "v1", "diagnoses", "analyze"]}
                    }
                },
                {
                    "name": "5.2 GET /api/v1/diagnoses - Retrieve Paginated Diagnosis History",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Status code is 200 OK', function () { pm.response.to.have.status(200); });",
                                "var res = pm.response.json();",
                                "pm.test('Diagnosis history returned with pagination', function () {",
                                "    pm.expect(res.success).to.be.true;",
                                "    pm.expect(res.data.content).to.be.an('array');",
                                "    pm.expect(res.data.totalElements).to.be.at.least(1);",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "GET",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{adminAccessToken}}"},
                            {"key": "Accept", "value": "application/json"}
                        ],
                        "url": {
                            "raw": "{{baseUrl}}/api/v1/diagnoses?page=0&size=10",
                            "host": ["{{baseUrl}}"],
                            "path": ["api", "v1", "diagnoses"],
                            "query": [
                                {"key": "page", "value": "0"},
                                {"key": "size", "value": "10"}
                            ]
                        }
                    }
                },
                {
                    "name": "5.3 GET /api/v1/diagnoses/{id} - Retrieve Diagnosis Session Detail",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Status code is 200 OK', function () { pm.response.to.have.status(200); });",
                                "var res = pm.response.json();",
                                "var diagId = res.data.diagnosisId || res.data.id;",
                                "pm.test('Diagnosis details match requested ID', function () {",
                                "    pm.expect(res.success).to.be.true;",
                                "    pm.expect(diagId).to.eql(pm.collectionVariables.get('createdDiagnosisId'));",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "GET",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{adminAccessToken}}"},
                            {"key": "Accept", "value": "application/json"}
                        ],
                        "url": {"raw": "{{baseUrl}}/api/v1/diagnoses/{{createdDiagnosisId}}", "host": ["{{baseUrl}}"], "path": ["api", "v1", "diagnoses", "{{createdDiagnosisId}}"]}
                    }
                },
                {
                    "name": "5.4 DELETE /api/v1/diagnoses/{id} - Soft Delete Diagnosis Session",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Diagnosis deleted with status 200 OK', function () { pm.response.to.have.status(200); });",
                                "var res = pm.response.json();",
                                "pm.test('Soft delete successful', function () {",
                                "    pm.expect(res.success).to.be.true;",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "DELETE",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{adminAccessToken}}"},
                            {"key": "Accept", "value": "application/json"}
                        ],
                        "url": {"raw": "{{baseUrl}}/api/v1/diagnoses/{{createdDiagnosisId}}", "host": ["{{baseUrl}}"], "path": ["api", "v1", "diagnoses", "{{createdDiagnosisId}}"]}
                    }
                }
            ]
        },

        # =====================================================================
        # 6. EDGE CASES & BOUNDARY GUARDS
        # =====================================================================
        {
            "name": "6. Edge Cases & Boundary Guards",
            "item": [
                {
                    "name": "6.1 GET /api/v1/diagnoses - Empty Filter Matching No Records",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Status code is 200 OK', function () { pm.response.to.have.status(200); });",
                                "var res = pm.response.json();",
                                "pm.test('Response structure is valid empty page', function () {",
                                "    pm.expect(res.success).to.be.true;",
                                "    pm.expect(res.data.totalElements).to.eql(0);",
                                "    pm.expect(res.data.content).to.be.an('array').that.is.empty;",
                                "    pm.expect(res.data.page).to.eql(0);",
                                "    pm.expect(res.data.size).to.eql(1);",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "GET",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{adminAccessToken}}"},
                            {"key": "Accept", "value": "application/json"}
                        ],
                        "url": {
                            "raw": "{{baseUrl}}/api/v1/diagnoses?plantCode=string&diseaseCode=string&status=PENDING&fromDate=2026-11-13T04:33:38.784Z&toDate=2026-11-13T04:33:38.784Z&page=0&size=1",
                            "host": ["{{baseUrl}}"],
                            "path": ["api", "v1", "diagnoses"],
                            "query": [
                                {"key": "plantCode", "value": "string"},
                                {"key": "diseaseCode", "value": "string"},
                                {"key": "status", "value": "PENDING"},
                                {"key": "fromDate", "value": "2026-11-13T04:33:38.784Z"},
                                {"key": "toDate", "value": "2026-11-13T04:33:38.784Z"},
                                {"key": "page", "value": "0"},
                                {"key": "size", "value": "1"}
                            ]
                        }
                    }
                },
                {
                    "name": "6.2 GET /api/v1/diagnoses - Invalid Sort Property Guard",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Status code is 400 Bad Request', function () { pm.response.to.have.status(400); });",
                                "var res = pm.response.json();",
                                "pm.test('Error code is REQ_001 with sort property rejection message', function () {",
                                "    pm.expect(res.success).to.be.false;",
                                "    pm.expect(res.code).to.eql('REQ_001');",
                                "    pm.expect(res.message).to.include('Invalid sort property');",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "GET",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{adminAccessToken}}"},
                            {"key": "Accept", "value": "application/json"}
                        ],
                        "url": {
                            "raw": "{{baseUrl}}/api/v1/diagnoses?sort=string",
                            "host": ["{{baseUrl}}"],
                            "path": ["api", "v1", "diagnoses"],
                            "query": [
                                {"key": "sort", "value": "string"}
                            ]
                        }
                    }
                },
                {
                    "name": "6.3 GET /api/v1/diagnoses/{id} - Malformed UUID Path Parameter",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Status code is 400 Bad Request', function () { pm.response.to.have.status(400); });",
                                "var res = pm.response.json();",
                                "pm.test('Error code is REQ_001 with format rejection message', function () {",
                                "    pm.expect(res.success).to.be.false;",
                                "    pm.expect(res.code).to.eql('REQ_001');",
                                "    pm.expect(res.message).to.include('Invalid parameter format');",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "GET",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{adminAccessToken}}"},
                            {"key": "Accept", "value": "application/json"}
                        ],
                        "url": {"raw": "{{baseUrl}}/api/v1/diagnoses/not-a-valid-uuid", "host": ["{{baseUrl}}"], "path": ["api", "v1", "diagnoses", "not-a-valid-uuid"]}
                    }
                },
                {
                    "name": "6.4 POST /api/v1/diseases - RBAC Security Guard (Non-Admin Rejection)",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Status code is 403 Forbidden', function () { pm.response.to.have.status(403); });",
                                "var res = pm.response.json();",
                                "pm.test('Error code is AUTH_003 with access denied', function () {",
                                "    pm.expect(res.success).to.be.false;",
                                "    pm.expect(res.code).to.eql('AUTH_003');",
                                "    pm.expect(res.message).to.include('Access denied');",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "POST",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{accessToken}}"},
                            {"key": "Content-Type", "value": "application/json"},
                            {"key": "Accept", "value": "application/json"}
                        ],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n    \"diseaseCode\": \"UNAUTHORIZED_DISEASE\",\n    \"vietnameseName\": \"Bệnh không hợp lệ\",\n    \"englishName\": \"Unauthorized Disease\",\n    \"severity\": \"HIGH\"\n}"
                        },
                        "url": {"raw": "{{baseUrl}}/api/v1/diseases", "host": ["{{baseUrl}}"], "path": ["api", "v1", "diseases"]}
                    }
                },
                {
                    "name": "6.5 POST /api/v1/auth/register - Validation Constraints on Empty Payload",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Status code is 400 Bad Request', function () { pm.response.to.have.status(400); });",
                                "var res = pm.response.json();",
                                "pm.test('Error code is REQ_001 with field-specific validation errors', function () {",
                                "    pm.expect(res.success).to.be.false;",
                                "    pm.expect(res.code).to.eql('REQ_001');",
                                "    pm.expect(res.data).to.have.property('username');",
                                "    pm.expect(res.data).to.have.property('email');",
                                "    pm.expect(res.data).to.have.property('password');",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "POST",
                        "header": [
                            {"key": "Content-Type", "value": "application/json"},
                            {"key": "Accept", "value": "application/json"}
                        ],
                        "body": {
                            "mode": "raw",
                            "raw": "{}"
                        },
                        "url": {"raw": "{{baseUrl}}/api/v1/auth/register", "host": ["{{baseUrl}}"], "path": ["api", "v1", "auth", "register"]}
                    }
                },
                {
                    "name": "6.6 GET /api/v1/diseases/{id} - Non-existent Entity Returns 404",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Status code is 404 Not Found', function () { pm.response.to.have.status(404); });",
                                "var res = pm.response.json();",
                                "pm.test('Error code is RES_001', function () {",
                                "    pm.expect(res.success).to.be.false;",
                                "    pm.expect(res.code).to.eql('RES_001');",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "GET",
                        "header": [
                            {"key": "Accept", "value": "application/json"}
                        ],
                        "url": {"raw": "{{baseUrl}}/api/v1/diseases/00000000-0000-0000-0000-000000000000", "host": ["{{baseUrl}}"], "path": ["api", "v1", "diseases", "00000000-0000-0000-0000-000000000000"]}
                    }
                }
            ]
        },
        # =====================================================================
        # 7. ADMIN USER MANAGEMENT & REDIS AUTH CACHE EVICTION (F4 + TASK 4.1)
        # =====================================================================
        {
            "name": "7. Admin RBAC & Redis Auth Cache Eviction",
            "item": [
                {
                    "name": "7.1 GET /api/v1/admin/users - List Users with Batch Providers & Diagnosis Counts",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Admin retrieves paginated user list (200 OK)', function () { pm.response.to.have.status(200); });",
                                "var res = pm.response.json();",
                                "pm.test('Batch DTO fields (identities & totalDiagnosesCount) are present', function () {",
                                "    pm.expect(res.success).to.be.true;",
                                "    pm.expect(res.data.content).to.be.an('array').and.not.be.empty;",
                                "    var first = res.data.content[0];",
                                "    pm.expect(first.id).to.be.a('string');",
                                "    pm.expect(first.linkedProviders).to.be.an('array');",
                                "    pm.expect(first.totalDiagnosesCount).to.be.a('number');",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "GET",
                        "header": [{"key": "Authorization", "value": "Bearer {{adminAccessToken}}"}],
                        "url": {
                            "raw": "{{baseUrl}}/api/v1/admin/users?page=0&size=10",
                            "host": ["{{baseUrl}}"],
                            "path": ["api", "v1", "admin", "users"],
                            "query": [
                                {"key": "page", "value": "0"},
                                {"key": "size", "value": "10"}
                            ]
                        }
                    }
                },
                {
                    "name": "7.2 GET /api/v1/admin/users - RBAC Non-Admin Forbidden (403 AUTH_003)",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Regular user rejected with 403 Forbidden', function () { pm.response.to.have.status(403); });",
                                "var res = pm.response.json();",
                                "pm.test('Error code is AUTH_003', function () {",
                                "    pm.expect(res.success).to.be.false;",
                                "    pm.expect(res.code).to.eql('AUTH_003');",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "GET",
                        "header": [{"key": "Authorization", "value": "Bearer {{accessToken}}"}],
                        "url": {"raw": "{{baseUrl}}/api/v1/admin/users", "host": ["{{baseUrl}}"], "path": ["api", "v1", "admin", "users"]}
                    }
                },
                {
                    "name": "7.3 GET /api/v1/admin/users/{id} - Retrieve Target User Detail",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Admin retrieves target user detail (200 OK)', function () { pm.response.to.have.status(200); });",
                                "var res = pm.response.json();",
                                "pm.test('User detail includes LOCAL provider', function () {",
                                "    pm.expect(res.success).to.be.true;",
                                "    pm.expect(res.data.id).to.eql(pm.collectionVariables.get('currentUserId'));",
                                "    pm.expect(res.data.linkedProviders).to.include('LOCAL');",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "GET",
                        "header": [{"key": "Authorization", "value": "Bearer {{adminAccessToken}}"}],
                        "url": {"raw": "{{baseUrl}}/api/v1/admin/users/{{currentUserId}}", "host": ["{{baseUrl}}"], "path": ["api", "v1", "admin", "users", "{{currentUserId}}"]}
                    }
                },
                {
                    "name": "7.4 GET /api/v1/admin/users/{id} - Non-Existent User Not Found (404 AUTH_008)",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Non-existent user returns 404 Not Found', function () { pm.response.to.have.status(404); });",
                                "var res = pm.response.json();",
                                "pm.expect(res.code).to.eql('AUTH_008');"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "GET",
                        "header": [{"key": "Authorization", "value": "Bearer {{adminAccessToken}}"}],
                        "url": {"raw": "{{baseUrl}}/api/v1/admin/users/00000000-0000-0000-0000-000000000000", "host": ["{{baseUrl}}"], "path": ["api", "v1", "admin", "users", "00000000-0000-0000-0000-000000000000"]}
                    }
                },
                {
                    "name": "7.5 PATCH /api/v1/admin/users/{adminId}/status - Self-Lockout Guard (400 AUTH_010)",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Admin cannot suspend own account (400 AUTH_010)', function () {",
                                "    pm.response.to.have.status(400);",
                                "    var res = pm.response.json();",
                                "    pm.expect(res.success).to.be.false;",
                                "    pm.expect(res.code).to.eql('AUTH_010');",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "PATCH",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{adminAccessToken}}"},
                            {"key": "Content-Type", "value": "application/json"}
                        ],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n    \"isActive\": false,\n    \"reason\": \"Self lockout edge-case guard check\"\n}"
                        },
                        "url": {"raw": "{{baseUrl}}/api/v1/admin/users/{{adminUserId}}/status", "host": ["{{baseUrl}}"], "path": ["api", "v1", "admin", "users", "{{adminUserId}}", "status"]}
                    }
                },
                {
                    "name": "7.6 PATCH /api/v1/admin/users/{adminId}/role - Self-Demote Guard (400 AUTH_010)",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Admin cannot demote own role (400 AUTH_010)', function () {",
                                "    pm.response.to.have.status(400);",
                                "    var res = pm.response.json();",
                                "    pm.expect(res.success).to.be.false;",
                                "    pm.expect(res.code).to.eql('AUTH_010');",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "PATCH",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{adminAccessToken}}"},
                            {"key": "Content-Type", "value": "application/json"}
                        ],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n    \"role\": \"ROLE_USER\"\n}"
                        },
                        "url": {"raw": "{{baseUrl}}/api/v1/admin/users/{{adminUserId}}/role", "host": ["{{baseUrl}}"], "path": ["api", "v1", "admin", "users", "{{adminUserId}}", "role"]}
                    }
                },
                {
                    "name": "7.7 PATCH /api/v1/admin/users/{id}/status - Suspend User & Evict Auth Cache",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Admin suspends target user (200 OK)', function () { pm.response.to.have.status(200); });",
                                "var res = pm.response.json();",
                                "pm.expect(res.data.isActive).to.be.false;"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "PATCH",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{adminAccessToken}}"},
                            {"key": "Content-Type", "value": "application/json"}
                        ],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n    \"isActive\": false,\n    \"reason\": \"Automated cache eviction security verification\"\n}"
                        },
                        "url": {"raw": "{{baseUrl}}/api/v1/admin/users/{{currentUserId}}/status", "host": ["{{baseUrl}}"], "path": ["api", "v1", "admin", "users", "{{currentUserId}}", "status"]}
                    }
                },
                {
                    "name": "7.8 Security Edge: Suspended User Cached JWT Immediately Rejected (401)",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Cached JWT is rejected immediately after suspension (401 AUTH_001)', function () {",
                                "    pm.response.to.have.status(401);",
                                "    var res = pm.response.json();",
                                "    pm.expect(res.success).to.be.false;",
                                "    pm.expect(res.code).to.eql('AUTH_001');",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "GET",
                        "header": [{"key": "Authorization", "value": "Bearer {{accessToken}}"}],
                        "url": {"raw": "{{baseUrl}}/api/v1/diagnoses", "host": ["{{baseUrl}}"], "path": ["api", "v1", "diagnoses"]}
                    }
                },
                {
                    "name": "7.9a PATCH /api/v1/admin/users/{id}/status - Re-Activate User",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Admin re-activates user (200 OK)', function () { pm.response.to.have.status(200); });",
                                "var res = pm.response.json();",
                                "pm.expect(res.data.isActive).to.be.true;"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "PATCH",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{adminAccessToken}}"},
                            {"key": "Content-Type", "value": "application/json"}
                        ],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n    \"isActive\": true,\n    \"reason\": \"Restored after suspension verification\"\n}"
                        },
                        "url": {"raw": "{{baseUrl}}/api/v1/admin/users/{{currentUserId}}/status", "host": ["{{baseUrl}}"], "path": ["api", "v1", "admin", "users", "{{currentUserId}}", "status"]}
                    }
                },
                {
                    "name": "7.9b PATCH /api/v1/admin/users/{id}/role - Promote to ROLE_EXPERT",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Admin promotes user to ROLE_EXPERT (200 OK)', function () { pm.response.to.have.status(200); });",
                                "var res = pm.response.json();",
                                "pm.expect(res.data.role).to.eql('ROLE_EXPERT');"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "PATCH",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{adminAccessToken}}"},
                            {"key": "Content-Type", "value": "application/json"}
                        ],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n    \"role\": \"ROLE_EXPERT\"\n}"
                        },
                        "url": {"raw": "{{baseUrl}}/api/v1/admin/users/{{currentUserId}}/role", "host": ["{{baseUrl}}"], "path": ["api", "v1", "admin", "users", "{{currentUserId}}", "role"]}
                    }
                },
                {
                    "name": "7.9c Cache Eviction Edge: Existing JWT Reflects ROLE_EXPERT Without Re-Login",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Promoted user passes RBAC guard & hits DTO validation (400 REQ_001, not 403)', function () {",
                                "    pm.response.to.have.status(400);",
                                "    var res = pm.response.json();",
                                "    pm.expect(res.code).to.eql('REQ_001');",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "POST",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{accessToken}}"},
                            {"key": "Content-Type", "value": "application/json"}
                        ],
                        "body": {"mode": "raw", "raw": "{}"},
                        "url": {"raw": "{{baseUrl}}/api/v1/diseases", "host": ["{{baseUrl}}"], "path": ["api", "v1", "diseases"]}
                    }
                },
                {
                    "name": "7.9d PATCH /api/v1/admin/users/{id}/role - Demote Back to ROLE_USER",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Admin restores ROLE_USER (200 OK)', function () { pm.response.to.have.status(200); });",
                                "var res = pm.response.json();",
                                "pm.expect(res.data.role).to.eql('ROLE_USER');"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "PATCH",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{adminAccessToken}}"},
                            {"key": "Content-Type", "value": "application/json"}
                        ],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n    \"role\": \"ROLE_USER\"\n}"
                        },
                        "url": {"raw": "{{baseUrl}}/api/v1/admin/users/{{currentUserId}}/role", "host": ["{{baseUrl}}"], "path": ["api", "v1", "admin", "users", "{{currentUserId}}", "role"]}
                    }
                }
            ]
        },
        # =====================================================================
        # 8. HYBRID GUEST MODE, REDIS IDEMPOTENCY (F1), IDOR & UPLOAD EDGE CASES
        # =====================================================================
        {
            "name": "8. Guest Mode, Idempotency Isolation & Upload Guards",
            "item": [
                {
                    "name": "8.1 User Analyze: First Upload Creates Diagnosis & Consumes Rate Slot 1/10",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Status code is 200 OK', function () { pm.response.to.have.status(200); });",
                                "var res = pm.response.json();",
                                "var diagId = res.data.diagnosisId || res.data.id;",
                                "pm.test('Response includes diagnosisId, treatments, and rate limit headers', function () {",
                                "    pm.expect(res.success).to.be.true;",
                                "    pm.expect(diagId).to.be.a('string');",
                                "    pm.expect(res.data.imageWidth).to.be.above(0);",
                                "    pm.expect(res.data.imageHeight).to.be.above(0);",
                                "    pm.expect(res.data.recommendedTreatments).to.be.an('array');",
                                "    if (res.data.boundingBoxes && res.data.boundingBoxes.length > 0) {",
                                "        var b = res.data.boundingBoxes[0];",
                                "        pm.expect(b.xMinNormalized).to.be.within(0, 1);",
                                "        pm.expect(b.yMinNormalized).to.be.within(0, 1);",
                                "        pm.expect(b.widthNormalized).to.be.within(0, 1);",
                                "        pm.expect(b.heightNormalized).to.be.within(0, 1);",
                                "    }",
                                "    pm.expect(res.data.latitude).to.eql(11.0528);",
                                "    pm.expect(pm.response.headers.get('X-RateLimit-Limit')).to.eql('10');",
                                "    pm.expect(pm.response.headers.get('X-RateLimit-Remaining')).to.eql('9');",
                                "    pm.collectionVariables.set('userDiagId', diagId);",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "POST",
                        "header": [{"key": "Authorization", "value": "Bearer {{accessToken}}"}],
                        "body": {
                            "mode": "formdata",
                            "formdata": [
                                {"key": "image", "type": "file", "src": "postman/test_leaf.jpg"},
                                {"key": "plantCode", "value": "TOMATO", "type": "text"},
                                {"key": "latitude", "value": "11.0528", "type": "text"},
                                {"key": "longitude", "value": "106.6661", "type": "text"},
                                {"key": "deviceInfo", "value": "Postman User1 Device", "type": "text"}
                            ]
                        },
                        "url": {"raw": "{{baseUrl}}/api/v1/diagnoses/analyze", "host": ["{{baseUrl}}"], "path": ["api", "v1", "diagnoses", "analyze"]}
                    }
                },
                {
                    "name": "8.2 Idempotency Hit (F1): Duplicate User Upload Replays Exact Diagnosis ID",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Duplicate authenticated upload returns 200 OK', function () { pm.response.to.have.status(200); });",
                                "var res = pm.response.json();",
                                "var diagId = res.data.diagnosisId || res.data.id;",
                                "pm.test('Redis idempotency replays exact same diagnosisId & decrements rate limit', function () {",
                                "    pm.expect(diagId).to.eql(pm.collectionVariables.get('userDiagId'));",
                                "    pm.expect(pm.response.headers.get('X-RateLimit-Remaining')).to.eql('8');",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "POST",
                        "header": [{"key": "Authorization", "value": "Bearer {{accessToken}}"}],
                        "body": {
                            "mode": "formdata",
                            "formdata": [
                                {"key": "image", "type": "file", "src": "postman/test_leaf.jpg"},
                                {"key": "plantCode", "value": "TOMATO", "type": "text"}
                            ]
                        },
                        "url": {"raw": "{{baseUrl}}/api/v1/diagnoses/analyze", "host": ["{{baseUrl}}"], "path": ["api", "v1", "diagnoses", "analyze"]}
                    }
                },
                {
                    "name": "8.3 DELETE /api/v1/diagnoses/{userDiagId} - Soft Delete Evicts Redis Idempotency",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Soft delete returns 200 OK', function () { pm.response.to.have.status(200); });",
                                "pm.expect(pm.response.json().success).to.be.true;"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "DELETE",
                        "header": [{"key": "Authorization", "value": "Bearer {{accessToken}}"}],
                        "url": {"raw": "{{baseUrl}}/api/v1/diagnoses/{{userDiagId}}", "host": ["{{baseUrl}}"], "path": ["api", "v1", "diagnoses", "{{userDiagId}}"]}
                    }
                },
                {
                    "name": "8.3b Idempotency Eviction Verified: Re-Upload Creates Fresh Diagnosis ID",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Re-upload after delete succeeds (200 OK)', function () { pm.response.to.have.status(200); });",
                                "var res = pm.response.json();",
                                "var diagId = res.data.diagnosisId || res.data.id;",
                                "pm.test('New diagnosisId differs from deleted cached ID', function () {",
                                "    pm.expect(diagId).to.be.a('string');",
                                "    pm.expect(diagId).to.not.eql(pm.collectionVariables.get('userDiagId'));",
                                "    pm.collectionVariables.set('userDiagId2', diagId);",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "POST",
                        "header": [{"key": "Authorization", "value": "Bearer {{accessToken}}"}],
                        "body": {
                            "mode": "formdata",
                            "formdata": [
                                {"key": "image", "type": "file", "src": "postman/test_leaf.jpg"},
                                {"key": "plantCode", "value": "TOMATO", "type": "text"}
                            ]
                        },
                        "url": {"raw": "{{baseUrl}}/api/v1/diagnoses/analyze", "host": ["{{baseUrl}}"], "path": ["api", "v1", "diagnoses", "analyze"]}
                    }
                },
                {
                    "name": "8.4 IDOR Zero-Trust Edge Case: Other User Cannot Read Private Diagnosis",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Cross-user access blocked with 404 DIAGNOSIS_001', function () {",
                                "    pm.response.to.have.status(404);",
                                "    var res = pm.response.json();",
                                "    pm.expect(res.success).to.be.false;",
                                "    pm.expect(res.code).to.eql('DIAGNOSIS_001');",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "GET",
                        "header": [{"key": "Authorization", "value": "Bearer {{adminAccessToken}}"}],
                        "url": {"raw": "{{baseUrl}}/api/v1/diagnoses/{{userDiagId2}}", "host": ["{{baseUrl}}"], "path": ["api", "v1", "diagnoses", "{{userDiagId2}}"]}
                    }
                },
                {
                    "name": "8.5 Hybrid Guest Analyze: Unauthenticated Request Allowed (Guest Slot 1/3)",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Guest diagnosis allowed without JWT (200 OK)', function () { pm.response.to.have.status(200); });",
                                "var res = pm.response.json();",
                                "var diagId = res.data.diagnosisId || res.data.id;",
                                "pm.test('Guest rate limit headers show 3 limit and 2 remaining', function () {",
                                "    pm.expect(res.success).to.be.true;",
                                "    pm.expect(diagId).to.be.a('string');",
                                "    pm.expect(pm.response.headers.get('X-RateLimit-Limit')).to.eql('3');",
                                "    pm.expect(pm.response.headers.get('X-RateLimit-Remaining')).to.eql('2');",
                                "    pm.collectionVariables.set('guestDiagId1', diagId);",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "POST",
                        "header": [],
                        "body": {
                            "mode": "formdata",
                            "formdata": [
                                {"key": "image", "type": "file", "src": "postman/test_leaf.jpg"},
                                {"key": "plantCode", "value": "TOMATO", "type": "text"}
                            ]
                        },
                        "url": {"raw": "{{baseUrl}}/api/v1/diagnoses/analyze", "host": ["{{baseUrl}}"], "path": ["api", "v1", "diagnoses", "analyze"]}
                    }
                },
                {
                    "name": "8.6 Guest Isolation (F1): Duplicate Guest Upload Never Cached (Guest Slot 2/3)",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Second guest upload succeeds (200 OK)', function () { pm.response.to.have.status(200); });",
                                "var res = pm.response.json();",
                                "var diagId = res.data.diagnosisId || res.data.id;",
                                "pm.test('Guest receives distinct diagnosisId (no idempotency replay)', function () {",
                                "    pm.expect(diagId).to.be.a('string');",
                                "    pm.expect(diagId).to.not.eql(pm.collectionVariables.get('guestDiagId1'));",
                                "    pm.expect(pm.response.headers.get('X-RateLimit-Remaining')).to.eql('1');",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "POST",
                        "header": [],
                        "body": {
                            "mode": "formdata",
                            "formdata": [
                                {"key": "image", "type": "file", "src": "postman/test_leaf.jpg"},
                                {"key": "plantCode", "value": "TOMATO", "type": "text"}
                            ]
                        },
                        "url": {"raw": "{{baseUrl}}/api/v1/diagnoses/analyze", "host": ["{{baseUrl}}"], "path": ["api", "v1", "diagnoses", "analyze"]}
                    }
                },
                {
                    "name": "8.7 Upload Magic-Byte Guard: Rejects Fake Image (400 FILE_002, Guest Slot 3/3)",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Corrupt/non-image file rejected with 400 FILE_002', function () {",
                                "    pm.response.to.have.status(400);",
                                "    var res = pm.response.json();",
                                "    pm.expect(res.success).to.be.false;",
                                "    pm.expect(res.code).to.eql('FILE_002');",
                                "    pm.expect(pm.response.headers.get('X-RateLimit-Remaining')).to.eql('0');",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "POST",
                        "header": [],
                        "body": {
                            "mode": "formdata",
                            "formdata": [
                                {"key": "image", "type": "file", "src": "postman/invalid_fake.jpg"}
                            ]
                        },
                        "url": {"raw": "{{baseUrl}}/api/v1/diagnoses/analyze", "host": ["{{baseUrl}}"], "path": ["api", "v1", "diagnoses", "analyze"]}
                    }
                },
                {
                    "name": "8.8 Guest Rate Limit (429 RATE_001) & Spoofed X-Forwarded-For Ignored (Slot 4/3)",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('4th guest call hits 429 RATE_001 despite spoofed X-Forwarded-For', function () {",
                                "    pm.response.to.have.status(429);",
                                "    var res = pm.response.json();",
                                "    pm.expect(res.success).to.be.false;",
                                "    pm.expect(res.code).to.eql('RATE_001');",
                                "    pm.expect(res.path).to.eql('/api/v1/diagnoses/analyze');",
                                "    pm.expect(pm.response.headers.get('X-RateLimit-Limit')).to.eql('3');",
                                "    pm.expect(pm.response.headers.get('X-RateLimit-Remaining')).to.eql('0');",
                                "    pm.expect(Number(pm.response.headers.get('Retry-After'))).to.be.at.least(1);",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "POST",
                        "header": [
                            {"key": "X-Forwarded-For", "value": "203.0.113.99"}
                        ],
                        "body": {
                            "mode": "formdata",
                            "formdata": [
                                {"key": "image", "type": "file", "src": "postman/invalid_fake.jpg"}
                            ]
                        },
                        "url": {"raw": "{{baseUrl}}/api/v1/diagnoses/analyze", "host": ["{{baseUrl}}"], "path": ["api", "v1", "diagnoses", "analyze"]}
                    }
                }
            ]
        },
        # =====================================================================
        # 9. OBSERVABILITY, CIRCUIT BREAKER (F5) & PAYLOAD VALIDATION EDGE CASES
        # =====================================================================
        {
            "name": "9. Circuit Breaker, Actuator RBAC & Error Contract",
            "item": [
                {
                    "name": "9.1 GET /actuator/circuitbreakers - Unauthenticated Blocked (401)",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Unauthenticated actuator request rejected (401)', function () { pm.response.to.have.status(401); });"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "GET",
                        "header": [],
                        "url": {"raw": "{{baseUrl}}/actuator/circuitbreakers", "host": ["{{baseUrl}}"], "path": ["actuator", "circuitbreakers"]}
                    }
                },
                {
                    "name": "9.2 GET /actuator/circuitbreakers - Non-Admin Forbidden (403)",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('ROLE_USER rejected on actuator (403)', function () { pm.response.to.have.status(403); });"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "GET",
                        "header": [{"key": "Authorization", "value": "Bearer {{accessToken}}"}],
                        "url": {"raw": "{{baseUrl}}/actuator/circuitbreakers", "host": ["{{baseUrl}}"], "path": ["actuator", "circuitbreakers"]}
                    }
                },
                {
                    "name": "9.3 GET /actuator/circuitbreakers - Admin Sees roboflowInference CLOSED (F5)",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Admin views circuitbreakers (200 OK)', function () { pm.response.to.have.status(200); });",
                                "var res = pm.response.json();",
                                "pm.test('roboflowInference breaker exists and is CLOSED', function () {",
                                "    pm.expect(res.circuitBreakers).to.be.an('object');",
                                "    pm.expect(res.circuitBreakers.roboflowInference.state).to.eql('CLOSED');",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "GET",
                        "header": [{"key": "Authorization", "value": "Bearer {{adminAccessToken}}"}],
                        "url": {"raw": "{{baseUrl}}/actuator/circuitbreakers", "host": ["{{baseUrl}}"], "path": ["actuator", "circuitbreakers"]}
                    }
                },
                {
                    "name": "9.4 GET /actuator/metrics - Admin Views Metric Names (200 OK)",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Admin views metrics (200 OK)', function () { pm.response.to.have.status(200); });",
                                "var res = pm.response.json();",
                                "pm.expect(res.names).to.be.an('array').and.not.be.empty;"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "GET",
                        "header": [{"key": "Authorization", "value": "Bearer {{adminAccessToken}}"}],
                        "url": {"raw": "{{baseUrl}}/actuator/metrics", "host": ["{{baseUrl}}"], "path": ["actuator", "metrics"]}
                    }
                },
                {
                    "name": "9.5 Malformed JSON Payload Returns 400 REQ_001",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Malformed JSON returns 400 REQ_001', function () {",
                                "    pm.response.to.have.status(400);",
                                "    var res = pm.response.json();",
                                "    pm.expect(res.success).to.be.false;",
                                "    pm.expect(res.code).to.eql('REQ_001');",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "POST",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{adminAccessToken}}"},
                            {"key": "Content-Type", "value": "application/json"}
                        ],
                        "body": {"mode": "raw", "raw": "{ \"diseaseCode\": \"BROKEN_JSON\""},
                        "url": {"raw": "{{baseUrl}}/api/v1/diseases", "host": ["{{baseUrl}}"], "path": ["api", "v1", "diseases"]}
                    }
                },
                {
                    "name": "9.6 GET /api/v1/admin/users - Invalid Sort Property Returns 400 REQ_001",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Invalid sort field on admin users returns 400 REQ_001', function () {",
                                "    pm.response.to.have.status(400);",
                                "    var res = pm.response.json();",
                                "    pm.expect(res.success).to.be.false;",
                                "    pm.expect(res.code).to.eql('REQ_001');",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "GET",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{adminAccessToken}}"},
                            {"key": "Accept", "value": "application/json"}
                        ],
                        "url": {
                            "raw": "{{baseUrl}}/api/v1/admin/users?sort=nonExistentField,desc",
                            "host": ["{{baseUrl}}"],
                            "path": ["api", "v1", "admin", "users"],
                            "query": [{"key": "sort", "value": "nonExistentField,desc"}]
                        }
                    }
                },
                {
                    "name": "9.7 GET /api/v1/admin/users - Filter by Keyword, Role & Active Status",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Filtered admin user query returns 200 OK', function () { pm.response.to.have.status(200); });",
                                "var res = pm.response.json();",
                                "pm.test('Filter isolates admin_eiu account accurately', function () {",
                                "    pm.expect(res.data.totalElements).to.be.at.least(1);",
                                "    pm.expect(res.data.content[0].username).to.eql('admin_eiu');",
                                "    pm.expect(res.data.content[0].role).to.eql('ROLE_ADMIN');",
                                "    pm.expect(res.data.content[0].isActive).to.be.true;",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "GET",
                        "header": [{"key": "Authorization", "value": "Bearer {{adminAccessToken}}"}],
                        "url": {
                            "raw": "{{baseUrl}}/api/v1/admin/users?keyword=admin_eiu&role=ROLE_ADMIN&isActive=true",
                            "host": ["{{baseUrl}}"],
                            "path": ["api", "v1", "admin", "users"],
                            "query": [
                                {"key": "keyword", "value": "admin_eiu"},
                                {"key": "role", "value": "ROLE_ADMIN"},
                                {"key": "isActive", "value": "true"}
                            ]
                        }
                    }
                }
            ]
        },
        # =====================================================================
        # 10. PR #4 & #5 CATALOG FILTERS, CONSTRAINTS & HISTORY COVERAGE
        # =====================================================================
        {
            "name": "10. Catalog Filters, Constraints & History Coverage",
            "item": [
                {
                    "name": "10.1 GET /api/v1/diseases - Filter by Keyword, Plant & Unpaged Mode",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Filtered disease catalog returns 200 OK', function () { pm.response.to.have.status(200); });",
                                "var res = pm.response.json();",
                                "pm.test('Unpaged filtered result matches TOMATO blight entries', function () {",
                                "    pm.expect(res.success).to.be.true;",
                                "    pm.expect(res.data.content).to.be.an('array').and.not.be.empty;",
                                "    var codes = res.data.content.map(function (d) { return d.diseaseCode; });",
                                "    pm.expect(codes).to.include('TOMATO_EARLY_BLIGHT');",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "GET",
                        "header": [{"key": "Accept", "value": "application/json"}],
                        "url": {
                            "raw": "{{baseUrl}}/api/v1/diseases?keyword=blight&plantCode=TOMATO&unpaged=true",
                            "host": ["{{baseUrl}}"],
                            "path": ["api", "v1", "diseases"],
                            "query": [
                                {"key": "keyword", "value": "blight"},
                                {"key": "plantCode", "value": "TOMATO"},
                                {"key": "unpaged", "value": "true"}
                            ]
                        }
                    }
                },
                {
                    "name": "10.2 GET /api/v1/diseases/{uuid} - Dual Lookup by UUID",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('UUID lookup returns 200 OK', function () { pm.response.to.have.status(200); });",
                                "var res = pm.response.json();",
                                "pm.expect(res.data.diseaseCode).to.eql('TOMATO_EARLY_BLIGHT');"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "GET",
                        "header": [{"key": "Accept", "value": "application/json"}],
                        "url": {"raw": "{{baseUrl}}/api/v1/diseases/{{sampleDiseaseId}}", "host": ["{{baseUrl}}"], "path": ["api", "v1", "diseases", "{{sampleDiseaseId}}"]}
                    }
                },
                {
                    "name": "10.3 GET /api/v1/plants/{plantCode} - Non-Existent Crop Returns 404 RES_001",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Missing plant code returns 404 RES_001', function () {",
                                "    pm.response.to.have.status(404);",
                                "    var res = pm.response.json();",
                                "    pm.expect(res.success).to.be.false;",
                                "    pm.expect(res.code).to.eql('RES_001');",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "GET",
                        "header": [{"key": "Accept", "value": "application/json"}],
                        "url": {"raw": "{{baseUrl}}/api/v1/plants/NON_EXISTENT_CROP", "host": ["{{baseUrl}}"], "path": ["api", "v1", "plants", "NON_EXISTENT_CROP"]}
                    }
                },
                {
                    "name": "10.4 POST /api/v1/diseases - Duplicate Disease Code Returns 409 RES_002",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Duplicate disease code returns 409 RES_002', function () {",
                                "    pm.response.to.have.status(409);",
                                "    var res = pm.response.json();",
                                "    pm.expect(res.success).to.be.false;",
                                "    pm.expect(res.code).to.eql('RES_002');",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "POST",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{adminAccessToken}}"},
                            {"key": "Content-Type", "value": "application/json"}
                        ],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n    \"diseaseCode\": \"TOMATO_EARLY_BLIGHT\",\n    \"commonName\": \"Duplicate Test Disease\",\n    \"severity\": \"LOW\"\n}"
                        },
                        "url": {"raw": "{{baseUrl}}/api/v1/diseases", "host": ["{{baseUrl}}"], "path": ["api", "v1", "diseases"]}
                    }
                },
                {
                    "name": "10.5 GET /api/v1/diseases/{id}/treatments?treatmentType=CHEMICAL - Filter by Type",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Filtered treatments query returns 200 OK', function () { pm.response.to.have.status(200); });",
                                "var res = pm.response.json();",
                                "pm.test('All returned treatments have type CHEMICAL', function () {",
                                "    pm.expect(res.data).to.be.an('array');",
                                "    res.data.forEach(function (t) { pm.expect(t.treatmentType).to.eql('CHEMICAL'); });",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "GET",
                        "header": [{"key": "Accept", "value": "application/json"}],
                        "url": {
                            "raw": "{{baseUrl}}/api/v1/diseases/{{sampleDiseaseId}}/treatments?treatmentType=CHEMICAL",
                            "host": ["{{baseUrl}}"],
                            "path": ["api", "v1", "diseases", "{{sampleDiseaseId}}", "treatments"],
                            "query": [{"key": "treatmentType", "value": "CHEMICAL"}]
                        }
                    }
                },
                {
                    "name": "10.6 POST /api/v1/diseases/{id}/treatments - ROLE_USER Forbidden (403 AUTH_003)",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('ROLE_USER blocked from creating treatment (403 AUTH_003)', function () {",
                                "    pm.response.to.have.status(403);",
                                "    var res = pm.response.json();",
                                "    pm.expect(res.code).to.eql('AUTH_003');",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "POST",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{accessToken}}"},
                            {"key": "Content-Type", "value": "application/json"}
                        ],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n    \"treatmentType\": \"BIOLOGICAL\",\n    \"title\": \"Unauthorized Treatment\",\n    \"instructions\": \"Blocked by RBAC\"\n}"
                        },
                        "url": {"raw": "{{baseUrl}}/api/v1/diseases/{{sampleDiseaseId}}/treatments", "host": ["{{baseUrl}}"], "path": ["api", "v1", "diseases", "{{sampleDiseaseId}}", "treatments"]}
                    }
                },
                {
                    "name": "10.7 PUT /api/v1/treatments/{id} - Missing Treatment Returns 404 RES_001",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Updating non-existent treatment returns 404 RES_001', function () {",
                                "    pm.response.to.have.status(404);",
                                "    var res = pm.response.json();",
                                "    pm.expect(res.code).to.eql('RES_001');",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "PUT",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{adminAccessToken}}"},
                            {"key": "Content-Type", "value": "application/json"}
                        ],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n    \"treatmentType\": \"PREVENTIVE\",\n    \"title\": \"Non Existent Treatment\",\n    \"instructions\": \"Should fail 404\"\n}"
                        },
                        "url": {"raw": "{{baseUrl}}/api/v1/treatments/00000000-0000-0000-0000-000000000000", "host": ["{{baseUrl}}"], "path": ["api", "v1", "treatments", "00000000-0000-0000-0000-000000000000"]}
                    }
                },
                {
                    "name": "10.8 GET /api/v1/diagnoses - Unauthenticated History Rejected (401 AUTH_001)",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Unauthenticated diagnosis history returns 401 AUTH_001', function () {",
                                "    pm.response.to.have.status(401);",
                                "    var res = pm.response.json();",
                                "    pm.expect(res.code).to.eql('AUTH_001');",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "GET",
                        "header": [{"key": "Accept", "value": "application/json"}],
                        "url": {"raw": "{{baseUrl}}/api/v1/diagnoses", "host": ["{{baseUrl}}"], "path": ["api", "v1", "diagnoses"]}
                    }
                },
                {
                    "name": "10.9 GET /api/v1/diagnoses - Positive History Filter Match (plantCode & status)",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Positive filtered diagnosis history returns 200 OK', function () { pm.response.to.have.status(200); });",
                                "var res = pm.response.json();",
                                "pm.test('Filtered page matches user COMPLETED TOMATO diagnosis', function () {",
                                "    pm.expect(res.data.totalElements).to.be.at.least(1);",
                                "    pm.expect(res.data.content[0].status).to.eql('COMPLETED');",
                                "    pm.expect(res.data.content[0].plant.plantCode).to.eql('TOMATO');",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "GET",
                        "header": [{"key": "Authorization", "value": "Bearer {{accessToken}}"}],
                        "url": {
                            "raw": "{{baseUrl}}/api/v1/diagnoses?plantCode=TOMATO&status=COMPLETED",
                            "host": ["{{baseUrl}}"],
                            "path": ["api", "v1", "diagnoses"],
                            "query": [
                                {"key": "plantCode", "value": "TOMATO"},
                                {"key": "status", "value": "COMPLETED"}
                            ]
                        }
                    }
                },
                {
                    "name": "10.10 POST /api/v1/diagnoses/analyze - Missing Image Multipart Rejected (400 REQ_001)",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Missing image field returns 400 REQ_001', function () {",
                                "    pm.response.to.have.status(400);",
                                "    var res = pm.response.json();",
                                "    pm.expect(res.success).to.be.false;",
                                "    pm.expect(res.code).to.eql('REQ_001');",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "POST",
                        "header": [{"key": "Authorization", "value": "Bearer {{adminAccessToken}}"}],
                        "body": {
                            "mode": "formdata",
                            "formdata": [
                                {"key": "plantCode", "value": "TOMATO", "type": "text"}
                            ]
                        },
                        "url": {"raw": "{{baseUrl}}/api/v1/diagnoses/analyze", "host": ["{{baseUrl}}"], "path": ["api", "v1", "diagnoses", "analyze"]}
                    }
                }
            ]
        },
        # =====================================================================
        # 11. TOKEN REUSE REPLAY ATTACK (AUTH_006) & AUTH RATE LIMIT (429 RATE_001)
        # =====================================================================
        {
            "name": "11. Token Reuse Detection & Auth Rate Limit",
            "item": [
                {
                    "name": "11.1 POST /api/v1/auth/login - Fresh Session for Replay Test (Auth 8/10)",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Fresh user login succeeds (200 OK)', function () { pm.response.to.have.status(200); });",
                                "var res = pm.response.json();",
                                "pm.collectionVariables.set('reuseTestRefreshToken', res.data.refreshToken);"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "POST",
                        "header": [{"key": "Content-Type", "value": "application/json"}],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n    \"emailOrUsername\": \"{{currentUsername}}\",\n    \"password\": \"{{currentPassword}}\",\n    \"deviceInfo\": \"Postman Reuse Test\"\n}"
                        },
                        "url": {"raw": "{{baseUrl}}/api/v1/auth/login", "host": ["{{baseUrl}}"], "path": ["api", "v1", "auth", "login"]}
                    }
                },
                {
                    "name": "11.2 POST /api/v1/auth/refresh - Consume & Rotate Refresh Token (Auth 9/10)",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('First refresh succeeds and rotates token (200 OK)', function () { pm.response.to.have.status(200); });",
                                "var res = pm.response.json();",
                                "pm.expect(res.data.refreshToken).to.not.eql(pm.collectionVariables.get('reuseTestRefreshToken'));",
                                "pm.collectionVariables.set('rotatedRefreshToken', res.data.refreshToken);"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "POST",
                        "header": [{"key": "Content-Type", "value": "application/json"}],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n    \"refreshToken\": \"{{reuseTestRefreshToken}}\"\n}"
                        },
                        "url": {"raw": "{{baseUrl}}/api/v1/auth/refresh", "host": ["{{baseUrl}}"], "path": ["api", "v1", "auth", "refresh"]}
                    }
                },
                {
                    "name": "11.3 POST /api/v1/auth/refresh - Replay Consumed Token Triggers AUTH_006 (Auth 10/10)",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('Replaying consumed refresh token triggers 401 AUTH_006', function () {",
                                "    pm.response.to.have.status(401);",
                                "    var res = pm.response.json();",
                                "    pm.expect(res.success).to.be.false;",
                                "    pm.expect(res.code).to.eql('AUTH_006');",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "POST",
                        "header": [{"key": "Content-Type", "value": "application/json"}],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n    \"refreshToken\": \"{{reuseTestRefreshToken}}\"\n}"
                        },
                        "url": {"raw": "{{baseUrl}}/api/v1/auth/refresh", "host": ["{{baseUrl}}"], "path": ["api", "v1", "auth", "refresh"]}
                    }
                },
                {
                    "name": "11.4 POST /api/v1/auth/login - Brute-Force Exceeds 10/min Limit (429 RATE_001)",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('11th POST /auth request in 60s hits 429 RATE_001', function () {",
                                "    pm.response.to.have.status(429);",
                                "    var res = pm.response.json();",
                                "    pm.expect(res.success).to.be.false;",
                                "    pm.expect(res.code).to.eql('RATE_001');",
                                "    pm.expect(pm.response.headers.get('X-RateLimit-Limit')).to.eql('10');",
                                "    pm.expect(pm.response.headers.get('X-RateLimit-Remaining')).to.eql('0');",
                                "    var retryAfter = Number(pm.response.headers.get('Retry-After'));",
                                "    pm.expect(retryAfter).to.be.at.least(1);",
                                "    pm.expect(retryAfter).to.be.at.most(60);",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "POST",
                        "header": [{"key": "Content-Type", "value": "application/json"}],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n    \"emailOrUsername\": \"{{currentUsername}}\",\n    \"password\": \"WrongPasswordBruteForce!\"\n}"
                        },
                        "url": {"raw": "{{baseUrl}}/api/v1/auth/login", "host": ["{{baseUrl}}"], "path": ["api", "v1", "auth", "login"]}
                    }
                },
                {
                    "name": "11.5 GET /api/v1/auth/me - GET Exempt From Auth POST Rate Limit (200 OK)",
                    "event": [{
                        "listen": "test",
                        "script": {
                            "exec": [
                                "pm.test('GET /auth/me remains 200 OK even when POST /auth is rate-limited', function () {",
                                "    pm.response.to.have.status(200);",
                                "    var res = pm.response.json();",
                                "    pm.expect(res.success).to.be.true;",
                                "    pm.expect(res.data.username).to.eql('admin_eiu');",
                                "});"
                            ],
                            "type": "text/javascript"
                        }
                    }],
                    "request": {
                        "method": "GET",
                        "header": [{"key": "Authorization", "value": "Bearer {{adminAccessToken}}"}],
                        "url": {"raw": "{{baseUrl}}/api/v1/auth/me", "host": ["{{baseUrl}}"], "path": ["api", "v1", "auth", "me"]}
                    }
                }
            ]
        }
    ]
}

os.makedirs('postman', exist_ok=True)
with open('postman/eiu_pdds_full_suite.postman_collection.json', 'w', encoding='utf-8') as f:
    json.dump(collection, f, indent=4)

print('Postman Collection generated successfully at postman/eiu_pdds_full_suite.postman_collection.json')
