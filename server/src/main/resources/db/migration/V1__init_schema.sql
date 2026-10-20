-- Flyway Migration: V1__init_schema.sql
-- Database: PostgreSQL 16 (Enterprise Foundation Schema)
-- Project: EIU Plant Disease Detection System (EIU PDDS)

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================================
-- 1. Users Table (Farmers, Agricultural Experts, Administrators)
-- ============================================================================
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username TEXT NOT NULL UNIQUE,
    email TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    full_name TEXT,
    phone_number TEXT,
    role TEXT NOT NULL DEFAULT 'ROLE_USER' CHECK (role IN ('ROLE_USER', 'ROLE_EXPERT', 'ROLE_ADMIN')),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0
);

-- ============================================================================
-- 2. Plants Table (Supported Crop Catalog)
-- ============================================================================
CREATE TABLE IF NOT EXISTS plants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plant_code TEXT NOT NULL UNIQUE,
    common_name TEXT NOT NULL,
    scientific_name TEXT,
    icon_url TEXT,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0
);

-- ============================================================================
-- 3. Diseases Table (Plant Diseases Catalog)
-- ============================================================================
CREATE TABLE IF NOT EXISTS diseases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    disease_code TEXT NOT NULL UNIQUE,
    common_name TEXT NOT NULL,
    scientific_name TEXT,
    symptoms TEXT,
    description TEXT,
    severity TEXT NOT NULL DEFAULT 'MODERATE' CHECK (severity IN ('LOW', 'MODERATE', 'HIGH', 'CRITICAL')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0
);

-- ============================================================================
-- 4. Plant Diseases Table (Many-to-Many Bridge Table)
-- ============================================================================
CREATE TABLE IF NOT EXISTS plant_diseases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plant_id UUID NOT NULL REFERENCES plants(id) ON DELETE CASCADE,
    disease_id UUID NOT NULL REFERENCES diseases(id) ON DELETE CASCADE,
    specific_symptoms TEXT,
    risk_season TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_plant_disease UNIQUE (plant_id, disease_id)
);

-- ============================================================================
-- 5. Treatments Table (Biological, Chemical, Preventive Protocols)
-- ============================================================================
CREATE TABLE IF NOT EXISTS treatments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    disease_id UUID NOT NULL REFERENCES diseases(id) ON DELETE CASCADE,
    plant_id UUID REFERENCES plants(id) ON DELETE SET NULL,
    treatment_type TEXT NOT NULL CHECK (treatment_type IN ('BIOLOGICAL', 'CHEMICAL', 'PREVENTIVE', 'CULTURAL')),
    title TEXT NOT NULL,
    instructions TEXT NOT NULL,
    active_ingredients TEXT,
    dosage_recommendation TEXT,
    safety_interval_days INTEGER CHECK (safety_interval_days >= 0),
    warning TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0
);

-- ============================================================================
-- 6. Diagnoses Table (Diagnosis Sessions & AI Metadata)
-- ============================================================================
CREATE TABLE IF NOT EXISTS diagnoses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    plant_id UUID REFERENCES plants(id) ON DELETE SET NULL,
    original_image_url TEXT NOT NULL,
    image_width INTEGER CHECK (image_width > 0),
    image_height INTEGER CHECK (image_height > 0),
    latitude DOUBLE PRECISION CHECK (latitude BETWEEN -90.0 AND 90.0),
    longitude DOUBLE PRECISION CHECK (longitude BETWEEN -180.0 AND 180.0),
    device_info TEXT,
    dominant_disease_id UUID REFERENCES diseases(id) ON DELETE SET NULL,
    dominant_confidence DOUBLE PRECISION CHECK (dominant_confidence BETWEEN 0.0 AND 1.0),
    inference_time_ms INTEGER,
    raw_ai_response JSONB NOT NULL DEFAULT '{}'::jsonb,
    status TEXT NOT NULL DEFAULT 'COMPLETED' CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0
);

-- ============================================================================
-- 7. Bounding Boxes Table (Individual AI Detections & Coordinates)
-- ============================================================================
CREATE TABLE IF NOT EXISTS bounding_boxes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    diagnosis_id UUID NOT NULL REFERENCES diagnoses(id) ON DELETE CASCADE,
    disease_id UUID REFERENCES diseases(id) ON DELETE SET NULL,
    label TEXT NOT NULL,
    confidence DOUBLE PRECISION NOT NULL CHECK (confidence BETWEEN 0.0 AND 1.0),
    x DOUBLE PRECISION NOT NULL,
    y DOUBLE PRECISION NOT NULL,
    width DOUBLE PRECISION NOT NULL CHECK (width > 0),
    height DOUBLE PRECISION NOT NULL CHECK (height > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================================
-- 8. Diagnosis Feedbacks Table (Human-in-the-loop: Farmer & Expert Review)
-- ============================================================================
CREATE TABLE IF NOT EXISTS diagnosis_feedbacks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    diagnosis_id UUID NOT NULL REFERENCES diagnoses(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    is_accurate BOOLEAN NOT NULL,
    suggested_disease_id UUID REFERENCES diseases(id) ON DELETE SET NULL,
    user_comment TEXT,
    expert_verified BOOLEAN NOT NULL DEFAULT FALSE,
    verified_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    verified_at TIMESTAMPTZ,
    expert_notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0
);

-- ============================================================================
-- Performance Indexes (Foreign Keys, Geospatial, Filter & JSONB GIN)
-- ============================================================================
-- Foreign Key Indexes (Avoid table locks and optimize JOIN queries)
CREATE INDEX IF NOT EXISTS idx_plant_diseases_plant_id ON plant_diseases(plant_id);
CREATE INDEX IF NOT EXISTS idx_plant_diseases_disease_id ON plant_diseases(disease_id);
CREATE INDEX IF NOT EXISTS idx_treatments_disease_id ON treatments(disease_id);
CREATE INDEX IF NOT EXISTS idx_treatments_plant_id ON treatments(plant_id);
CREATE INDEX IF NOT EXISTS idx_diagnoses_user_id ON diagnoses(user_id);
CREATE INDEX IF NOT EXISTS idx_diagnoses_plant_id ON diagnoses(plant_id);
CREATE INDEX IF NOT EXISTS idx_diagnoses_dominant_disease ON diagnoses(dominant_disease_id);
CREATE INDEX IF NOT EXISTS idx_bounding_boxes_diagnosis_id ON bounding_boxes(diagnosis_id);
CREATE INDEX IF NOT EXISTS idx_bounding_boxes_disease_id ON bounding_boxes(disease_id);
CREATE INDEX IF NOT EXISTS idx_feedbacks_diagnosis_id ON diagnosis_feedbacks(diagnosis_id);
CREATE INDEX IF NOT EXISTS idx_feedbacks_user_id ON diagnosis_feedbacks(user_id);
CREATE INDEX IF NOT EXISTS idx_feedbacks_suggested_disease ON diagnosis_feedbacks(suggested_disease_id);
CREATE INDEX IF NOT EXISTS idx_feedbacks_verified_by ON diagnosis_feedbacks(verified_by_user_id);

-- Query Access Path Indexes
CREATE INDEX IF NOT EXISTS idx_diagnoses_created_at ON diagnoses(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_diagnoses_user_created ON diagnoses(user_id, created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_diagnoses_geo ON diagnoses(latitude, longitude) WHERE latitude IS NOT NULL AND longitude IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_diagnoses_raw_ai_gin ON diagnoses USING GIN (raw_ai_response);
