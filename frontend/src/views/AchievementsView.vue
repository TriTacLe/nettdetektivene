<script setup lang="ts">

import SceneComponent from "@/components/SceneComponent.vue";
import BadgeCard from "@/components/BadgeCard.vue";
</script>

<template>
  <SceneComponent title="Prestasjoner" previous="/student-home" background="/trophy.jpg">
    <div class="achievements-container">
      <div class="achievements-card">
        <h1>Mine prestasjoner</h1>



        <!-- Bruker info -->
        <section class="section">
          <div class="profile-info">
            <p><strong>Detektiv:</strong> <!-- brukernavn --></p>
            <p><strong>Level:</strong> <!-- Level --></p>
            <p><strong>Exp:</strong> <!-- 75 /100 --></p>
          </div>
        </section>

        <!-- total fremgang -->
        <section class="section">
          <h2>Total progresjon</h2>
          <div class="total-progress">
            <div class="progress-info">
              <p><strong>Fullført:</strong> <!-- Hent info fra backend "7/33" --></p>
            </div>

            <div class="progress-bar-container">
              <div class="progress-bar">
                <div class="progress-fill">
                  <!-- Hent info fra backend og fyll det inn i <style> -->
                </div>
              </div>
              <div class="progress-percent">
                <!-- Hent info fra backend -->
              </div>
            </div>
          </div>
        </section>

        <!-- Medaljer -->
        <section class="section">
          <h2>Medaljer</h2>
          <div class="badges-container">
            <BadgeCard
              title="Fullført Nyhetskvartalet"
              description="Fullfør alle oppgaver i Nyhetskvartalet"
              :unlocked="true"
            />

            <BadgeCard
              title="Perfeksjonist"
              description="Fullfør et stoppested uten feil"
              :unlocked="false"
            />

            <BadgeCard
              title="På sporet"
              description="Du har fullført 2 stoppesteder"
              :unlocked="false"
            />

            <BadgeCard
              title="Nybegynner detektiv"
              description="du har nådd level 2"
              :unlocked="true"
            />

            <BadgeCard
              title="Fullført fotoboksen"
              description="Fullfør alle oppgaver i fotoboksen"
              :unlocked="false"
            />
            <BadgeCard
              title="Erfaren detektiv"
              description="Du har nådd level 5"
              :unlocked="false"
            />

            <BadgeCard
              title="Arrestert datatyven"
              description="Du har fullført stoppestedet datasenteret"
              :unlocked="false"
            />
          </div>
        </section>


      </div>
    </div>
  </SceneComponent>
</template>

<style scoped>
.achievements-container {
  display: flex;
  justify-content: center;
  padding: 1rem;
  flex: 1;
  min-height: 0;

}

.achievements-card {
  width: 100%;
  max-width: 900px;
  background: white;
  border-radius: 16px;
  padding: 2rem;
  overflow-y: auto;
  max-height: 100%;
}

.achievements-card h1 {
  text-align: center;
  margin-bottom: 0.75rem;
}

.section {
  margin-bottom: 0.75rem;
}

.section h2 {
  margin-bottom: 0.75rem;
}

.badges-container {
  min-height: 100px;
  border: 1px dashed #000000;
  border-radius: 12px;
  padding: 0.75rem;
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  align-items: center;
}

.profile-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;

  border: 1px solid #ddd;
  border-radius: 12px;
  background-color: #f8f8f8;
}

.profile-info p {
  margin: 0.5rem 0;
}



.total-progress {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 2rem;

  border: 1px solid #ddd;
  border-radius: 12px;
  padding: 1rem;
  background-color: #f8f8f8;
}

.progress-info {
  white-space: nowrap;
}

.progress-bar-container {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  width: 100%;
}

.progress-bar {
  flex: 1;
  height: 20px;
  background-color: #ddd;
  border-radius: 10px;
  overflow: hidden;
}

.progress-fill {
  width: 0%; /* settes senere */
  height: 100%;
  background-color: #4caf50;
  transition: width 0.3s ease;
}

.progress-percent {
  min-width: 40px;
  text-align: right;
  font-weight: bold;
}

.achievements-card::-webkit-scrollbar-track {
  background: transparent;
  margin-block: 12px;
}
.achievements-card::-webkit-scrollbar {
  width: 10px;
}
.achievements-card::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.2);
  border-radius: 999px;
}
</style>
