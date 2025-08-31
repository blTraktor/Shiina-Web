<div class="col-12  col-lg-<#if bigBm??>12<#else>6</#if> flex-column">

    <div class="osu-beatmap-card">
        <div class="osu-beatmap-container">
            <!-- Beatmap Background -->
            <div class="osu-beatmap-bg" style="background-image: url('https://assets.ppy.sh/beatmaps/${beatmap.set_id?c}/covers/cover.jpg?1650681317')"></div>

            <!-- Content Overlay -->
            <div class="osu-beatmap-content">
                <!-- Status Badge -->
                <div class="osu-beatmap-status">
                    <span class="status-badge bg-${convertStatusBackColor(beatmap.status)}">
                        ${convertStatusBack(beatmap.status)}
                    </span>
                </div>
                
                <!-- Beatmap Info -->
                <div class="osu-beatmap-info">
                    <div class="osu-beatmap-title mt-2">
                        ${beatmap.filename?replace(".osu", "")}
                    </div>
                    <div class="osu-beatmap-creator">
                        mapped by ${beatmap.creator}
                    </div>
                    
                    <div class="osu-beatmap-stats">
                        <div class="stat-item">
                            <i class="fa-solid fa-star"></i>
                            <span>${beatmap.diff?string("0.00")}</span>
                        </div>
                        <div class="stat-item">
                            <i class="fa-solid fa-play"></i>
                            <span>${beatmap.plays}</span>
                        </div>
                        <div class="stat-item">
                            <i class="fa-solid fa-circle-check"></i>
                            <span>${beatmap.passes}</span>
                        </div>
                        <#if beatmap.last_update??>
                        <div class="stat-item">
                            <i class="fa-solid fa-calendar"></i>
                            <span data-timestamp-format="date" data-timestamp="${beatmap.last_update}">
                                ${beatmap.last_update}
                            </span>
                        </div>
                        </#if>
                    </div>
                </div>
                
                <!-- Action Buttons -->
                <div class="osu-beatmap-actions">
                    <a href="/b/${beatmap.id?c}" 
                       class="osu-action-btn osu-view-btn" 
                       title="View Beatmap"
                       onmouseover="this.style.background='rgba(52,152,219,0.3)'; this.style.borderColor='#3498db';"
                       onmouseout="this.style.background='rgba(255,255,255,0.1)'; this.style.borderColor='rgba(255,255,255,0.2)';">
                        <i class="fas fa-eye"></i>
                    </a>
                    <a download href="https://osu.direct/api/d/${beatmap.set_id?c}"
                       class="osu-action-btn osu-download-btn" 
                       title="Download Beatmap"
                       onmouseover="this.style.background='rgba(46,204,113,0.3)'; this.style.borderColor='#2ecc71';"
                       onmouseout="this.style.background='rgba(255,255,255,0.1)'; this.style.borderColor='rgba(255,255,255,0.2)';">
                        <i class="fas fa-download"></i>
                    </a>
                </div>
            </div>
        </div>
    </div>
</div>