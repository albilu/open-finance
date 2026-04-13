CREATE TABLE real_estate_simulations (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id             INTEGER NOT NULL,
    name                VARCHAR(200) NOT NULL,
    simulation_type     VARCHAR(20) NOT NULL,
    data                TEXT NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_simulation_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_simulation_type_valid
        CHECK (simulation_type IN ('buy_rent', 'rental_investment')),

    CONSTRAINT chk_simulation_name_not_empty
        CHECK (LENGTH(TRIM(name)) > 0),

    CONSTRAINT chk_simulation_data_not_empty
        CHECK (LENGTH(TRIM(data)) > 0)
);

CREATE INDEX idx_simulation_user_id ON real_estate_simulations(user_id);
CREATE INDEX idx_simulation_type ON real_estate_simulations(simulation_type);
CREATE INDEX idx_simulation_user_type ON real_estate_simulations(user_id, simulation_type);
CREATE INDEX idx_simulation_name ON real_estate_simulations(user_id, name);