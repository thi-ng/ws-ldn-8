// emcc -s ASM_JS=1 particles.c -s INVOKE_RUN=0 -o resources/public/js/native.html

// emcc -O2 -o index.html -s ASM_JS=1 -s NO_FILESYSTEM=1 --closure 1 particles.c
// emcc -O2 -o index.html -s NO_FILESYSTEM=1 -s INVOKE_RUN=1 -s "EXPORT_NAME='Foo'" -s MODULARIZE=1 --closure 1 particles.c

#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <math.h>
#include <emscripten.h>

#define MIN(X,Y) ((X) < (Y) ? (X) : (Y))

typedef struct {
  float x,y,z;
} Vec3;

typedef struct {
  Vec3 pos; // 12 bytes
  Vec3 vel; // 12 bytes
  //Vec3 col; // 12 bytes
} Particle; // 2 * 12

typedef struct {
  Particle *particles;
  uint32_t numParticles;
  uint32_t maxParticles;
  Vec3 emitPos;
  Vec3 emitDir;
  Vec3 gravity;
  float speed;
  uint32_t age;
  uint32_t maxAge;
} ParticleSystem;

static inline void setVec3(Vec3 *v, float x, float y, float z) {
  v->x = x;
  v->y = y;
  v->z = z;
}

static inline void addVec3(Vec3* v, Vec3* v2) {
  v->x += v2->x;
  v->y += v2->y;
  v->z += v2->z;
}

static inline void scaleVec3(Vec3* v, float s) {
  v->x *= s;
  v->y *= s;
  v->z *= s;
}

static inline void normalizeVec3(Vec3* v, float l) {
  float m = sqrtf(v->x * v->x + v->y * v->y + v->z * v->z);
  if (m > 0.0f) {
    l /= m;
    v->x *= l;
    v->y *= l;
    v->z *= l;
  }
}

static inline float randf01() {
  return (float)rand() / (float)RAND_MAX;
}

static inline float randf() {
  return randf01() * 2.0f - 1.0f;
}

static void emitParticle(ParticleSystem* psys) {
  Particle *p = &psys->particles[psys->numParticles];
  p->pos = psys->emitPos;
  p->vel = psys->emitDir;
  scaleVec3(&p->vel, 1.0f + randf01() * 5.0f);
  p->vel.x += randf();
  p->vel.z += randf();
  scaleVec3(&p->vel, psys->speed);
  //p->col = (uint32_t)rand();
  psys->numParticles++;
}

EMSCRIPTEN_KEEPALIVE uint32_t getNumParticles(ParticleSystem* psys) {
  return psys->numParticles;
}

EMSCRIPTEN_KEEPALIVE Particle* getParticlesPointer(ParticleSystem* psys) {
  return psys->particles;
}

EMSCRIPTEN_KEEPALIVE float getParticleComponent(ParticleSystem* psys, uint32_t idx, uint32_t component) {
  Vec3 *pos = &(psys->particles[idx]).pos;
  switch(component) {
  case 0: return pos->x;
  case 1: return pos->y;
  case 2: return pos->z;
  default: return 0;
  }
}

static ParticleSystem* makeParticleSystem(uint32_t num) {
  ParticleSystem *psys = (ParticleSystem*)malloc(sizeof(ParticleSystem));
  psys->particles = (Particle*)malloc(num * sizeof(Particle));
  psys->maxParticles = num;
  psys->numParticles = 0;
  return psys;
}

EMSCRIPTEN_KEEPALIVE ParticleSystem* initParticleSystem(uint32_t num, uint32_t maxAge, float emitX, float gravityY, float speed) {
  ParticleSystem *psys = makeParticleSystem(num);
  setVec3(&(psys->emitPos), emitX, 1.f, 0.f);
  setVec3(&(psys->emitDir), 0.f, 1.f, 0.f);
  setVec3(&(psys->gravity), 0.f, gravityY, 0.f);
  psys->maxAge = maxAge;
  psys->speed = speed;
  return psys;
}

EMSCRIPTEN_KEEPALIVE ParticleSystem* updateParticleSystem(ParticleSystem* psys) {
  if (psys->age == psys->maxAge) {
    psys->numParticles = 0;
    psys->age = 0;
  }
  if (psys->numParticles < psys->maxParticles) {
    uint32_t limit = MIN(psys->numParticles + 10, psys->maxParticles);
    while(psys->numParticles < limit) {
      emitParticle(psys);
    }
  }
  for(uint32_t i=0; i < psys->numParticles; i++) {
    Particle *p = &psys->particles[i];
    addVec3(&p->pos, &p->vel);
    addVec3(&p->vel, &psys->gravity);
    if (p->pos.y < 0) {
      p->pos.y = 0;
      p->vel.y *= -0.88f;
    }
  }
  psys->age++;
  return psys;
}

int main(int argc, char** argv) {
  printf("Hello Emscripten!\n");
  printf("Particle size: %u\n", sizeof(Particle));
  return 0;
}
