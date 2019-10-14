/*
 *
 * Copyright (c) 2017, Graz Robust and Intelligent Production System (grips)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package com.grips.model.teamserver.dao;

import com.grips.model.teamserver.GameState;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;

@Repository
public interface GameStateDao extends CrudRepository<GameState, Long> {

    public GameState findTop1ByOrderByGameTimeNanoSecondsDesc();

    @Query(value = "SELECT grips_color " +
            "FROM game_state " +
            "ORDER BY id DESC " +
            "LIMIT 1", nativeQuery = true)
    public String getTeamColor();

    @Query(value = "SELECT phase FROM game_state ORDER BY id DESC LIMIT 1", nativeQuery = true)
    public GameState.GameStatePhase getGamePhase();

    @Query(value = "SELECT state FROM game_state ORDER BY id DESC LIMIT 1", nativeQuery = true)
    public GameState.GameStateState getGameState();

    @Query(value = "select game_time_nano_seconds from game_state where phase like \"EXPLORATION\" order by game_time_nano_seconds desc limit 1", nativeQuery = true)
    public BigInteger getLatestGameTimeExplorationPhase();

    @Query(value = "select game_time_nano_seconds from game_state where phase like \"PRODUCTION\" order by game_time_nano_seconds desc limit 1", nativeQuery = true)
    public BigInteger getLatestGameTimeProductionPhase();

    @Query(value = "SELECT points_cyan FROM game_state ORDER BY id DESC LIMIT 1", nativeQuery = true)
    public int getCurrentPointsCyan();

    @Query(value = "SELECT points_magenta FROM game_state ORDER BY id DESC LIMIT 1", nativeQuery = true)
    public int getCurrentPointsMagenta();
}
